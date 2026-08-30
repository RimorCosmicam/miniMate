package com.minimate.ui.shader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import com.minimate.touchpad.engine.TouchPoint
import com.minimate.touchpad.model.AbstractShaderTheme
import com.minimate.touchpad.model.BackgroundTheme
import com.minimate.touchpad.model.ShaderRecolor
import com.minimate.touchpad.model.ThemeFilter
import com.minimate.touchpad.model.subthemesFor

/**
 * The procedural sections are adaptations of MIT sources documented in THIRD_PARTY_NOTICES.md.
 * Every scene is generated in AGSL from geometry, fields, and simulation state; no scene bitmap is sampled.
 */
private const val SOURCED_SHADER = """
uniform float2 uResolution;
uniform float uTime;
uniform float uNow;
uniform float uTheme;
uniform float uSubtheme;
uniform float uVariant;
uniform float uPalette;
uniform float uReaction;
uniform float uFilter;
uniform float3 uSceneColor0;
uniform float3 uSceneColor1;
uniform float3 uSceneColor2;
uniform float3 uSceneColor3;
uniform float2 uTouches[8];
uniform float uTouchStarts[8];
uniform float uTouchActive[8];
uniform float uTouchCount;
uniform float2 uTilt;
uniform float2 uAngularVelocity;
uniform shader glyphAtlas;

float hash(float2 p){return fract(sin(dot(p,float2(127.1,311.7)))*43758.5453123);}
float2 hash2(float2 p){return fract(sin(float2(dot(p,float2(127.1,311.7)),dot(p,float2(269.5,183.3))))*43758.5453);}
float noise(float2 p){float2 i=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);return mix(mix(hash(i),hash(i+float2(1,0)),f.x),mix(hash(i+float2(0,1)),hash(i+1.0),f.x),f.y);}
float fbm(float2 p){float v=0.0;v+=noise(p)*.5;p=p*2.03+17.1;v+=noise(p)*.25;p=p*2.01+9.7;v+=noise(p)*.125;p=p*2.04+5.3;return v+noise(p)*.0625;}
float2 rot(float2 p,float a){float c=cos(a),s=sin(a);return float2(c*p.x-s*p.y,s*p.x+c*p.y);}
float line(float d,float w){return smoothstep(w,0.0,abs(d));}
float sdBox(float2 p,float2 b){float2 d=abs(p)-b;return length(max(d,0.0))+min(max(d.x,d.y),0.0);}

float touchEnergy(){float e=0.0;for(int i=0;i<8;i++){if(float(i)<uTouchCount){float age=max(0.0,uNow-uTouchStarts[i]);e+=max(uTouchActive[i],exp(-age*1.45));}}return min(e,2.0);}
float ripple(float2 uv){float v=0.0;for(int i=0;i<8;i++){if(float(i)<uTouchCount){float age=max(0.0,uNow-uTouchStarts[i]);float d=distance(uv,uTouches[i]);float life=max(uTouchActive[i],exp(-age*1.25));v+=sin(d*52.0-age*8.0)*life*exp(-d*5.0);}}return v;}
float2 touchWarp(float2 uv){float2 q=uv;for(int i=0;i<8;i++){if(float(i)<uTouchCount){float age=max(0.0,uNow-uTouchStarts[i]);float2 d=q-uTouches[i];float l=max(length(d),.008);float life=max(uTouchActive[i],exp(-age*1.55));float wave=sin(l*48.0-age*7.0)*exp(-l*7.0)*life;float pull=-exp(-l*11.0)*life;float amount=uReaction<.5?wave*.009:(uReaction<1.5?pull*.018:(uReaction<2.5?wave*.014:pull*.01));q+=d/l*amount;}}return q;}
float touchMemory(float2 uv){float m=0.0;for(int i=0;i<8;i++){if(float(i)<uTouchCount){float d=distance(uv,uTouches[i]);m+=exp(-d*9.0)*(.48+.52*uTouchActive[i]);}}return clamp(m,0.0,1.5);}
float2 touchMemoryFlow(float2 uv){float2 flow=float2(0.0);for(int i=0;i<8;i++){if(float(i)<uTouchCount){float2 d=uv-uTouches[i];float l=max(length(d),.012);float falloff=exp(-l*8.0)*(.45+.55*uTouchActive[i]);flow+=float2(-d.y,d.x)/l*falloff;}}return flow;}
float2 touchFocus(float2 uv){float2 q=uv;for(int i=0;i<8;i++){if(float(i)<uTouchCount){float2 d=q-uTouches[i];float l=max(length(d),.01);q=uTouches[i]+d*(1.0-.32*exp(-l*8.0));}}return q;}
float2 latestTouch(){float2 target=float2(.5);for(int i=0;i<8;i++){if(float(i)<uTouchCount)target=uTouches[i];}return target;}

float3 pal3(float v){v=clamp(v,0.0,1.0);if(v<.333)return mix(uSceneColor0,uSceneColor1,v*3.0);if(v<.666)return mix(uSceneColor1,uSceneColor2,(v-.333)*3.0);return mix(uSceneColor2,uSceneColor3,(v-.666)*3.0);}
float box(float2 p,float2 c,float2 b){return smoothstep(.012,0.0,sdBox(p-c,b));}
float disc(float2 p,float2 c,float r){return smoothstep(r,r-.012,length(p-c));}
float segment(float2 p,float2 a,float2 b,float w){float2 pa=p-a,ba=b-a;float h=clamp(dot(pa,ba)/max(dot(ba,ba),.0001),0.0,1.0);return smoothstep(w,w*.42,length(pa-ba*h));}
float starField(float2 uv,float density,float speed){float2 g=uv*density+float2(0,uTime*speed),id=floor(g),f=fract(g)-.5;float2 pos=(hash2(id)-.5)*.75;float h=hash(id+17.0);float d=length(f-pos);return step(.72,h)*smoothstep(.075+.055*h,.006,d)*(.45+.55*sin(uTime*(1.0+h*2.0)+h*30.0));}
float terrain(float2 p){return fbm(p*2.4)+.45*fbm(p*7.0)+.16*fbm(p*19.0);}
float voronoi(float2 x){float2 n=floor(x),f=fract(x);float md=8.0;for(int j=-1;j<=1;j++)for(int i=-1;i<=1;i++){float2 g=float2(float(i),float(j));float2 o=hash2(n+g);o=.5+.5*sin(uTime*.25+6.2831*o);md=min(md,length(g+o-f));}return md;}

half3 spaceScene(float2 uv,int s){float2 q=touchWarp(uv)+touchMemoryFlow(uv)*.018,p=(q-.5)*float2(uResolution.x/uResolution.y,1.0);float t=uTime;float stars=starField(q,24.0,.012)+.5*starField(q,47.0,-.005);float v=0.0;
    if(s==0){float flight=0.0;for(int i=0;i<5;i++){float fi=float(i);float z=fract(fi*.2+t*.055);float fade=smoothstep(.0,.12,z)*smoothstep(1.0,.82,z);float scale=mix(11.0,.72,z);float2 suv=(q-.5)*scale+.5+hash2(float2(fi,9.0))*.17;float layer=starField(suv,31.0+fi*7.0,0.0);flight+=layer*fade*(.35+z*.9);}return half3(pal3(clamp(flight,0.0,1.0)));}
    if(s==1){float2 g=rot(p,-.28);g.y/=.55;float r=length(g),a=atan(g.y,g.x);float phase=2.0*(a+t*.08)+log(max(r,.025))*6.8;float arm=pow(.5+.5*cos(phase),3.0)*smoothstep(.72,.08,r)*smoothstep(.035,.16,r);float clump=.35+.8*fbm(rot(g,-phase*.08)*18.0);float core=exp(-r*r*55.0);return half3(pal3(clamp(stars*.2+arm*clump+core,0.0,1.0)));}
    if(s==2){float r=length(p),a=atan(p.y,p.x);float lens=.075/max(r,.055);float lensedStars=starField(q+normalize(p)*lens,33.0,.004)*smoothstep(.19,.27,r);float2 d=rot(p,-.13)*float2(1.0,5.6);float disk=exp(-abs(length(d)-.34)*24.0)*smoothstep(.14,.21,r);float dop=.18+.92*smoothstep(-.75,.75,cos(a+.13));float photon=exp(-abs(r-.176)*125.0);float shadow=smoothstep(.148,.174,r);float value=(lensedStars*.42+disk*(.75+1.2*dop)+photon*1.35)*shadow;return half3(pal3(clamp(value,0.0,1.0)));}
    if(s==3){float n=fbm(p*2.2+float2(t*.018,-t*.012));float filament=pow(clamp(fbm(p*5.0+n*2.5)-.42,0.0,1.0),1.5);return half3(pal3(clamp(stars*.32+filament*1.5,0.0,1.0)));}
    if(s==4){float2 rp=rot(p-float2(.08,0),-.24);float sphere=disc(rp,float2(0),.29);float bands=.5+.5*sin(rp.y*55.0+fbm(rp*9.0)*7.0+t*.04);float rr=length(rp*float2(1.0,4.1));float ringBands=line(rr-.5,.035)+line(rr-.59,.022)+line(rr-.67,.014)+line(rr-.73,.007);float rear=ringBands*step(rp.y,0.0)*(1.0-sphere),front=ringBands*step(0.0,rp.y);float light=max(0.0,.35+rp.x*1.8);return half3(pal3(clamp(stars*.18+rear*.48+sphere*(.22+.55*bands)*light+front*.72,0.0,1.0)));}
    if(s==5){float2 c=p-float2(.04,.02);float r=length(c),sphere=smoothstep(.34,.33,r);float lon=atan(c.y,c.x)+t*.025;float land=smoothstep(.49,.6,fbm(float2(lon*3.0,c.y*9.0)));float clouds=smoothstep(.57,.7,fbm(float2(lon*5.0+t*.018,c.y*15.0)));float limb=exp(-abs(r-.335)*65.0);return half3(pal3(clamp(stars*.15+sphere*(.2+.45*land)+clouds*sphere*.35+limb*.7,0.0,1.0)));}
    if(s==6){float horizon=-.08;float ground=step(horizon,p.y);float depth=max(.04,p.y-horizon);float2 world=float2(p.x/(depth+.18),(1.0/(depth+.18))+t*.28);float h=terrain(world*1.3);float craters=pow(1.0-voronoi(world*3.4),9.0);float ridge=smoothstep(.46,.7,h)*smoothstep(.3,-.02,depth);float earth=disc(p,float2(.31,-.29),.075);return half3(pal3(clamp(stars*(1.0-ground)+earth+ridge*.45+ground*(.12+.42*h+.32*craters),0.0,1.0)));}
    if(s==7){float sky=stars*.7;float mountain=step(.18+.12*fbm(float2(p.x*2.0,4.0)),p.y);float curtain=0.0;for(int i=0;i<5;i++){float fi=float(i);float x=-.5+fi*.24+.12*sin(p.y*3.0+t*.08+fi);curtain+=exp(-abs(p.x-x)*18.0)*smoothstep(.28,-.38,p.y)*(.5+.5*sin(p.y*15.0+t*.2+fi));}return half3(pal3(clamp(sky+curtain*.75+mountain*.18,0.0,1.0)));}
    if(s==8){float r=length(p),a=atan(p.y,p.x);float shell=.3+.055*fbm(float2(a*3.0,t*.03))+.02*sin(a*13.0+t*.2);float fil=exp(-abs(r-shell)*48.0)*(.25+.9*noise(float2(a*18.0,t*.05)));float ejecta=pow(max(0.0,1.0-r/.42),2.0)*(.18+.45*fbm(float2(a*5.0,r*14.0-t*.08)));float core=exp(-r*r*85.0);float knots=pow(noise(float2(a*31.0,r*22.0-t*.06)),8.0)*fil;return half3(pal3(clamp(stars*.2+ejecta+fil*.8+knots+core,0.0,1.0)));}
    float2 ship=p-float2(.06,.045*sin(t*.7));float core=box(ship,float2(0),float2(.2,.045));float nose=box(rot(ship-float2(.19,0),.785),float2(0),float2(.06,.06));float cabin=box(ship,float2(.07,-.055),float2(.07,.04));float truss=box(ship,float2(-.16,0),float2(.16,.012));float panels=box(ship,float2(-.14,.15),float2(.2,.09))+box(ship,float2(-.14,-.15),float2(.2,.09));float grid=(line(fract((ship.x+.34)*30.0)-.5,.07)+line(fract((ship.y+.26)*30.0)-.5,.07))*panels;float modules=disc(ship,float2(-.03,0),.055)+disc(ship,float2(-.12,0),.045);float engine=exp(-length((ship-float2(-.34,0))*float2(1.0,4.0))*32.0);float craft=core+nose+truss+panels*.42+grid*.5+modules+engine;return half3(pal3(clamp(stars*.62+craft,0.0,1.0)));
}

float alien(float2 p,float2 c,float z){float2 g=floor((p-c)/z+float2(2.5));if(g.x<0.0||g.x>4.0||g.y<0.0||g.y>4.0)return 0.0;float x=abs(g.x-2.0),y=g.y;float on=0.0;if(y<.5)on=step(x,1.0);else if(y<1.5)on=step(x,2.0)-step(x,.5);else if(y<2.5)on=1.0;else if(y<3.5)on=step(.5,x);else on=1.0-step(x,.5);return on;}
float cuteFace(float2 p,float2 c,float z){float eyes=disc(p,c+float2(-z*.22,-z*.08),z*.07)+disc(p,c+float2(z*.22,-z*.08),z*.07);float smile=line(length((p-c-float2(0,z*.08))*float2(1.0,1.8))-z*.22,z*.035)*step(c.y+z*.08,p.y);return eyes+smile;}
float triWave(float x){return abs(fract(x)*2.0-1.0);}
float pixelCar(float2 p,float2 c,float z){float body=box(p,c,float2(z*.8,z*1.25));float cabin=box(p,c-float2(0,z*.28),float2(z*.55,z*.42));float wheels=box(p,c+float2(-z*.88,z*.62),float2(z*.15,z*.28))+box(p,c+float2(z*.88,z*.62),float2(z*.15,z*.28));return body+cabin+wheels;}
float pixelFrog(float2 p,float2 c,float z){float body=box(p,c,float2(z*.42,z*.38));float eyes=disc(p,c+float2(-z*.29,-z*.42),z*.18)+disc(p,c+float2(z*.29,-z*.42),z*.18);float legs=box(p,c+float2(-z*.58,z*.3),float2(z*.23,z*.13))+box(p,c+float2(z*.58,z*.3),float2(z*.23,z*.13));return body+eyes+legs;}
float ghostSprite(float2 p,float2 c,float z){float head=disc(p,c-float2(0,z*.16),z*.48);float skirt=box(p,c+float2(0,z*.2),float2(z*.48,z*.34));float feet=disc(p,c+float2(-z*.32,z*.5),z*.17)+disc(p,c+float2(0,z*.5),z*.17)+disc(p,c+float2(z*.32,z*.5),z*.17);return head+skirt+feet;}
half3 arcadeScene(float2 uv,int s){
    float2 p=(uv-.5)*float2(uResolution.x/uResolution.y,1.0);p=floor(p*192.0)/192.0;
    float2 target=(latestTouch()-.5)*float2(uResolution.x/uResolution.y,1.0);float touched=step(.5,uTouchCount),t=uTime;
    float3 c=uSceneColor0;
    if(s==0){
        float loop=mod(t,12.0),stepN=floor(loop*2.0),round=floor(t/12.0);float2 g=floor((p+float2(.34,.48))*float2(14.0,20.0));
        float board=step(0.0,g.x)*step(g.x,9.0)*step(0.0,g.y)*step(g.y,19.0);float frame=box(p,float2(-.07,0),float2(.335,.49))-board;
        float stackTop=15.0-floor(hash(float2(g.x,round))*5.0);float filled=board*step(stackTop,g.y)*step(.13,hash(g+round*7.0));
        float shape=mod(round,4.0),fallY=min(14.0,stepN),autoX=2.0+mod(round*3.0,6.0),touchX=clamp(floor((target.x+.34)*14.0),0.0,8.0),fallX=mix(autoX,touchX,touched);
        float falling=0.0;if(shape<.5)falling=step(abs(g.x-fallX),1.0)*step(abs(g.y-fallY),.1);else if(shape<1.5)falling=step(abs(g.x-fallX),.1)*step(abs(g.y-fallY),1.0);else if(shape<2.5)falling=step(abs(g.y-fallY),.1)*step(abs(g.x-fallX-.5),1.5);else falling=step(abs(g.x-fallX),1.0)*step(abs(g.y-fallY),1.0)*step(1.1,abs(g.x-fallX)+abs(g.y-fallY));
        float grid=(line(fract((p.x+.34)*14.0)-.5,.025)+line(fract((p.y+.48)*20.0)-.5,.025))*board;float clear=step(10.7,loop)*step(17.0,g.y)*step(g.y,18.0);
        c=mix(c,uSceneColor1,board*.72+frame*.45);c=mix(c,uSceneColor2,clamp(filled+falling,0.0,1.0));c=mix(c,uSceneColor3,clear+grid*.18);
    }else if(s==1){
        float roadCenter=.13*sin(p.y*2.8+t*.32)+.045*sin(p.y*8.0-t*.2);float width=.18+.38*(p.y+.5);float road=smoothstep(width,width-.025,abs(p.x-roadCenter));float shoulder=line(abs(p.x-roadCenter)-width,.016);float lane=line(p.x-roadCenter,.012)*step(.52,fract((p.y+t*.48)*10.0));
        float trees=0.0;for(int i=0;i<10;i++){float fi=float(i),y=fract(fi*.137+t*.11)-.5,x=roadCenter+sign(sin(fi*9.0))*(.31+.4*(y+.5));trees+=disc(p,float2(x,y),.025+.035*(y+.5));}
        float rivalY=.28-triWave(t*.075)*.58,rivalX=roadCenter+.13*sin(t*.6);float playerX=mix(roadCenter-.11,clamp(target.x,-.35,.35),touched);float player=pixelCar(p,float2(playerX,.29),.048),rival=pixelCar(p,float2(rivalX,rivalY),.04);
        c=mix(c,uSceneColor1,clamp((1.0-road)*(.32+trees*.65)+road*.32,0.0,1.0));c=mix(c,uSceneColor3,shoulder+lane);c=mix(c,uSceneColor2,clamp(player+rival*.75,0.0,1.0));
    }else if(s==2){
        float match=mod(t,18.0),leg=floor(match/3.0),u=smoothstep(0.0,1.0,fract(match/3.0));float2 a=float2(-.42+.14*mod(leg,3.0),-.24+.16*mod(leg,4.0));float2 b=float2(.42-.11*mod(leg,4.0),.22-.13*mod(leg,3.0));float2 ball=mix(a,b,u);if(touched>.5)ball=target;
        float pitch=box(p,float2(0),float2(.62,.46));float stripe=.08*step(.5,fract((p.x+.65)*8.0));float marks=line(abs(p.x)-.6,.008)+line(abs(p.y)-.44,.008)+line(p.x,.007)+line(length(p)-.105,.007);float goals=line(abs(p.x)-.61,.014)*step(abs(p.y),.14);
        float home=0.0,away=0.0;for(int i=0;i<10;i++){float fi=float(i),side=fi<5.0?-1.0:1.0;float row=mod(fi,5.0);float2 formation=float2(side*(.12+.1*mod(row,3.0)),-.3+row*.15);float2 pc=mix(formation,ball,.25+.1*hash(float2(fi,2.0)));float sprite=disc(p,pc,.027)+box(p,pc+float2(0,.033),float2(.019,.025));if(side<0.0)home+=sprite;else away+=sprite;}
        c=mix(c,uSceneColor1,pitch*(.55+stripe));c=mix(c,uSceneColor3,clamp(marks+goals+disc(p,ball,.019),0.0,1.0));c=mix(c,uSceneColor2,clamp(home+away*.58,0.0,1.0));
    }else if(s==3){
        float run=mod(t,16.0),bx=triWave(run*.19)*.82-.41,by=triWave(run*.137)*.72-.34;float paddleX=mix(bx,clamp(target.x,-.36,.36),touched);float2 ball=float2(bx,by);float2 g=floor((p+float2(.45,.43))*float2(11.0,16.0));float bricks=0.0;if(g.x>=0.0&&g.x<10.0&&g.y>=0.0&&g.y<6.0){float hit=hash(g)*13.0;bricks=step(run,hit)*step(.08,fract((p.x+.45)*11.0))*step(.12,fract((p.y+.43)*16.0));}
        float cabinet=box(p,float2(0),float2(.48,.47));float paddle=box(p,float2(paddleX,.4),float2(.12,.018));float impact=exp(-abs(run-mod(hash(floor((ball+float2(.45,.43))*16.0))*13.0,13.0))*10.0);
        c=mix(c,uSceneColor1,cabinet*.28);c=mix(c,uSceneColor2,bricks);c=mix(c,uSceneColor3,clamp(paddle+disc(p,ball,.02)+impact*disc(p,ball,.055),0.0,1.0));
    }else if(s==4){
        float round=mod(t,20.0),kill=floor(round*.72),march=floor(mod(round*2.0,12.0))*.01-.055,drop=floor(round/5.0)*.035;float swarm=0.0,blast=0.0;
        for(int y=0;y<4;y++)for(int x=0;x<8;x++){float idx=float(x+y*8);float2 ac=float2(-.53+float(x)*.15,-.34+float(y)*.105+drop)+float2(march,0);float alive=step(kill,idx);swarm+=alien(p,ac,.022)*alive;blast+=disc(p,ac,.04)*step(abs(idx-kill),.1)*step(.68,fract(round*.72));}
        float playerX=mix(-.36+.72*triWave(round*.11),clamp(target.x,-.43,.43),touched);float player=box(p,float2(playerX,.42),float2(.073,.018))+box(p,float2(playerX,.393),float2(.018,.035));float shotX=-.53+mod(kill,8.0)*.15+march,shotY=.36-fract(round*.72)*.68;float shot=box(p,float2(shotX,shotY),float2(.005,.022));float shields=0.0;for(int i=0;i<4;i++){float x=-.42+float(i)*.28;shields+=disc(p,float2(x,.28),.067)-box(p,float2(x,.315),float2(.025,.035));}
        c=mix(c,uSceneColor1,clamp(shields*.48,0.0,1.0));c=mix(c,uSceneColor2,clamp(swarm+player,0.0,1.0));c=mix(c,uSceneColor3,clamp(shot+blast,0.0,1.0));
    }else if(s==5){
        float2 cell=floor((p+float2(.54,.48))*float2(17.0,15.0));float border=step(cell.x,.1)+step(15.9,cell.x)+step(cell.y,.1)+step(13.9,cell.y);float posts=step(mod(cell.x,4.0),.1)*step(1.0,mod(cell.y,5.0))+step(mod(cell.y,4.0),.1)*step(1.0,mod(cell.x,6.0));float maze=clamp(border+posts,0.0,1.0);
        float phase=mod(t,16.0),seg=floor(phase/4.0),u=fract(phase/4.0);float2 a=seg<.5?float2(-.44,.31):(seg<1.5?float2(.4,.31):(seg<2.5?float2(.4,-.3):float2(-.44,-.3)));float2 b=seg<.5?float2(.4,.31):(seg<1.5?float2(.4,-.3):(seg<2.5?float2(-.44,-.3):float2(-.44,.31)));float2 hero=mix(a,b,u);float dots=(1.0-maze)*step(.72,hash(cell))*step(phase*.055,hash(cell+9.0));float pac=disc(p,hero,.04),mouth=step(.75,dot(normalize(p-hero),normalize(b-a)))*pac;float ghosts=0.0;for(int j=0;j<3;j++){float fj=float(j);float2 gc=float2(.28*sin(t*.3+fj*2.0),.24*cos(t*.22+fj));ghosts+=ghostSprite(p,gc,.065);}
        c=mix(c,uSceneColor1,maze*.72);c=mix(c,uSceneColor3,dots+(pac-mouth));c=mix(c,uSceneColor2,ghosts);
    }else if(s==6){
        float cycle=mod(t,13.0),hop=floor(cycle/.72),hopEase=smoothstep(0.0,.32,fract(cycle/.72));float2 route=float2(.13*sin(hop*2.1),.43-min(hop,10.0)*.086);route.y-=.018*sin(hopEase*3.14159);if(touched>.5)route=target;
        float river=step(-.04,p.y)*step(p.y,.29),road=step(-.43,p.y)*step(p.y,-.12),bank=1.0-clamp(river+road,0.0,1.0);float traffic=0.0,logs=0.0;
        for(int i=0;i<4;i++){float fi=float(i),dir=mod(fi,2.0)<1.0?1.0:-1.0,x=fract(t*(.1+.02*fi)+fi*.27)*1.55-.78;if(dir<0.0)x=-x;traffic+=pixelCar(p,float2(x,-.39+fi*.075),.025);}
        for(int j=0;j<4;j++){float fj=float(j),dir=mod(fj,2.0)<1.0?1.0:-1.0,x=fract(t*(.035+.008*fj)+fj*.31)*1.6-.8;if(dir<0.0)x=-x;logs+=box(p,float2(x,-.0+fj*.082),float2(.16,.022))+disc(p,float2(x-dir*.15,fj*.082),.023);}
        c=mix(c,uSceneColor1,road*.36+river*.62+bank*.3);c=mix(c,uSceneColor2,traffic);c=mix(c,uSceneColor3,clamp(logs+pixelFrog(p,route,.07),0.0,1.0));
    }else if(s==7){
        float2 g=floor((p+float2(.46))*16.0);float tick=floor(mod(t*4.0,56.0));float leg=floor(tick/14.0),u=mod(tick,14.0);float2 head=leg<.5?float2(1.0+u,2.0):(leg<1.5?float2(15.0-u,6.0):(leg<2.5?float2(1.0+u,10.0):float2(15.0-u,14.0)));float snake=0.0;for(int i=0;i<13;i++){float fi=float(i),sx=head.x-fi*(leg<.5||leg>1.5?1.0:-1.0);snake+=step(length(g-float2(mod(sx+14.0,14.0)+1.0,head.y)),.1);}float2 appleCell=float2(3.0+mod(floor(t/14.0)*5.0,11.0),4.0+mod(floor(t/14.0)*3.0,9.0));float apple=step(length(g-appleCell),.1);float board=box(p,float2(0),float2(.46));float grid=(line(fract((p.x+.46)*16.0)-.5,.02)+line(fract((p.y+.46)*16.0)-.5,.02))*board;
        c=mix(c,uSceneColor1,board*.45+grid*.12);c=mix(c,uSceneColor2,snake);c=mix(c,uSceneColor3,apple+step(length(g-head),.1));
    }else if(s==8){
        float phase=mod(t,12.0),seg=floor(phase/2.0),u=smoothstep(0.0,1.0,fract(phase/2.0));float2 a=seg<.5?float2(.34,.4):(seg<1.5?float2(.18,-.12):(seg<2.5?float2(-.2,-.2):(seg<3.5?float2(.02,.12):(seg<4.5?float2(-.3,.18):float2(.0,.4)))));float2 b=seg<.5?float2(.18,-.12):(seg<1.5?float2(-.2,-.2):(seg<2.5?float2(.02,.12):(seg<3.5?float2(-.3,.18):(seg<4.5?float2(.0,.4):float2(.34,.4)))));float2 ball=mix(a,b,u);
        float table=box(p,float2(0),float2(.45,.47)),rail=line(abs(p.x)-.43,.013)+line(abs(p.y)-.45,.013);float bump=disc(p,float2(-.2,-.18),.068)+disc(p,float2(.18,-.08),.068)+disc(p,float2(0,.12),.06);float rings=line(length(p-float2(-.2,-.18))-.083,.009)+line(length(p-float2(.18,-.08))-.083,.009);float hit=disc(p,b,.1)*step(.72,u);float flip=sin(t*5.0)*.18;float flippers=box(rot(p-float2(-.12,.34),-.22-flip),float2(0),float2(.11,.018))+box(rot(p-float2(.12,.34),.22+flip),float2(0),float2(.11,.018));
        c=mix(c,uSceneColor1,table*.38+rail);c=mix(c,uSceneColor2,bump+flippers);c=mix(c,uSceneColor3,clamp(rings+hit+disc(p,ball,.019),0.0,1.0));
    }else{
        float cycle=mod(t,18.0),scroll=cycle*.12;float groundY=.28+.035*sin((p.x+scroll)*6.0);float ground=step(groundY,p.y);float obstacleX=.52-mod(scroll,1.15),jump=pow(max(0.0,sin((cycle-1.2)*1.7)),2.0);float2 hero=float2(-.24,.22-.15*jump);if(touched>.5)hero.x=clamp(target.x,-.4,.25);float heroSprite=box(p,hero,float2(.038,.055))+box(p,hero-float2(0,.06),float2(.05,.04))+box(p,hero+float2(-.045,.065),float2(.022,.018))+box(p,hero+float2(.045,.065),float2(.022,.018));float enemy=ghostSprite(p,float2(obstacleX,.235),.055);float blocks=0.0,coins=0.0;for(int i=0;i<7;i++){float fi=float(i),x=.65-mod(scroll+fi*.27,1.4);blocks+=box(p,float2(x,.08+.1*step(.5,mod(fi,2.0))),float2(.055));coins+=disc(p,float2(x,-.04+.07*sin(fi)),.018)*step(.12,abs(x-hero.x));}float hills=.5+.5*sin((p.x+scroll*.2)*4.0)+.2*sin((p.x+scroll*.1)*11.0);
        c=mix(c,uSceneColor1,ground*.55+smoothstep(.35,.8,hills)*(1.0-ground)*.22);c=mix(c,uSceneColor2,clamp(heroSprite+enemy+blocks*.6,0.0,1.0));c=mix(c,uSceneColor3,coins);
    }
    c*=.96+.04*sin(uv.y*uResolution.y*1.5708);return half3(clamp(c,0.0,1.0));
}

float waveSum(float2 p,float t){float w=0.0;w+=sin(dot(p,float2(.82,.57))*7.0+t*.72)*.48;w+=sin(dot(p,float2(-.38,.92))*11.0+t*.94)*.26;w+=sin(dot(p,float2(.96,.28))*17.0+t*1.21)*.14;w+=sin(dot(p,float2(-.7,.71))*23.0+t*1.47)*.08;return w;}
float fish(float2 p,float2 c,float z,float dir){float body=smoothstep(1.0,.82,length((p-c)*float2(1.0/z,1.8/z)));float2 tailP=p-(c-float2(dir*z*.9,0));float tail=smoothstep(.25,0.0,abs(tailP.y)-abs(tailP.x)*.7)*step(0.0,-tailP.x*dir)*step(abs(tailP.x),z*.55);return body+tail;}
half3 beachScene(float2 uv,int s){float2 q=touchWarp(uv)+touchMemoryFlow(uv)*.026,p=(q-.5)*float2(uResolution.x/uResolution.y,1.0);float t=uTime,v=0.0;
    float3 c=uSceneColor0;
    if(s==0){
        float cape=.09+.13*sin(p.x*2.2+.4)+.055*sin(p.x*7.0-t*.08)-.22*exp(-pow((p.x-.23)*3.2,2.0));float signedCoast=p.y-cape;float land=smoothstep(-.015,.025,signedCoast),shallows=smoothstep(.28,-.08,abs(signedCoast))*(1.0-land);float wet=smoothstep(.02,.12,signedCoast)*smoothstep(.27,.11,signedCoast);float depth=.5+.25*fbm(p*3.0+float2(t*.012,0));
        float foam=0.0;for(int i=0;i<4;i++){float fi=float(i),front=cape-.035-fi*.07+.018*sin(p.x*(8.0+fi*2.0)-t*(.45+.08*fi));foam+=exp(-abs(p.y-front)*(55.0-fi*6.0))*(.25+.75*noise(float2(p.x*46.0-fi,t*.9)));}
        float rocks=0.0;for(int j=0;j<12;j++){float fj=float(j),x=-.6+fj*.11,y=.26+.08*sin(fj*2.1);rocks+=disc(p,float2(x,y),.012+.018*hash(float2(fj,3.0)))*land;}
        c=mix(uSceneColor0,uSceneColor1,depth);c=mix(c,uSceneColor2,shallows*.72);c=mix(c,uSceneColor3,land);c=mix(c,uSceneColor2,wet*.45);c=mix(c,uSceneColor3,clamp(foam+rocks*.4,0.0,1.0));
    }else if(s==1){
        float horizon=-.075,sky=1.0-smoothstep(horizon-.008,horizon+.008,p.y),sea=1.0-sky;float y=max(.02,p.y-horizon),perspective=1.0/(y+.18);float waves=waveSum(float2(p.x*(1.0+y*2.0),p.y*5.0),t*.72);float crests=pow(max(0.0,waves),7.0)*sea;float sun=disc(p,float2(.29,-.29),.075);float clouds=0.0;for(int i=0;i<5;i++){float fi=float(i),x=-.58+fi*.29+.035*sin(t*.04+fi);clouds+=disc(p,float2(x,-.3+.035*sin(fi)),.065)+disc(p,float2(x+.05,-.3),.045);}
        float boat=segment(p,float2(-.3,-.01),float2(-.15,-.01),.018)+segment(p,float2(-.225,-.01),float2(-.225,-.12),.006);float sail=smoothstep(.012,0.0,sdBox(rot(p-float2(-.19,-.07),-.35),float2(.035,.06)));
        c=mix(uSceneColor3,uSceneColor2,clamp((p.y+.5)*1.4,0.0,1.0));c=mix(c,mix(uSceneColor0,uSceneColor1,.6+.18*waves),sea);c=mix(c,uSceneColor3,clamp(sun+clouds*.3+crests*.65,0.0,1.0));c=mix(c,uSceneColor0,boat+sail*.65);
    }else if(s==2){
        float caustic=pow(abs(sin(fbm(p*8.0+float2(t*.04,0))*19.0)),14.0);float reef=0.0,coral=0.0,polyps=0.0;
        for(int i=0;i<10;i++){float fi=float(i),x=-.62+fi*.14,base=.4-.05*hash(float2(fi,2.0));reef+=disc(p,float2(x,base),.07+.04*hash(float2(fi,7.0)));for(int j=0;j<5;j++){float fj=float(j),a=-2.45+fj*.35+.12*sin(fi),len=.08+.045*hash(float2(fi,fj));float2 root=float2(x,base-.04),tip=root+float2(cos(a),sin(a))*len;coral+=segment(p,root,tip,.011+.004*hash(float2(fj,fi)));polyps+=disc(p,tip,.012);}}
        float school=0.0,stripes=0.0;for(int k=0;k<9;k++){float fk=float(k),dir=mod(fk,2.0)<1.0?1.0:-1.0;float x=fract(t*(.024+.003*fk)+fk*.16)*1.5-.75;if(dir<0.0)x=-x;float2 fc=float2(x,-.32+mod(fk,4.0)*.11);school+=fish(p,fc,.024+.006*mod(fk,3.0),dir);stripes+=segment(p,fc-float2(dir*.008,.018),fc+float2(dir*.008,.018),.005);}
        c=mix(uSceneColor0,uSceneColor1,.45+.18*fbm(p*3.0));c=mix(c,uSceneColor3,caustic*.32+reef*.28);c=mix(c,uSceneColor2,clamp(coral+polyps+school*.8,0.0,1.0));c=mix(c,uSceneColor3,stripes);
    }else if(s==3){
        float2 poolP=p*float2(.86,1.0);float poolShape=1.0-smoothstep(.37,.43,length(poolP+float2(.02,.01))+.055*fbm(p*7.0));float rim=smoothstep(.43,.5,length(poolP)+.04*fbm(p*8.0));float stones=0.0,shells=0.0;
        for(int i=0;i<16;i++){float fi=float(i);float2 sc=(hash2(float2(fi,8.0))-.5)*float2(1.12,.82);float rr=.018+.025*hash(float2(fi,3.0));stones+=disc(p,sc,rr)*poolShape;shells+=line(length(p-sc)-rr*.65,.005)*step(.72,hash(float2(fi,12.0)))*poolShape;}
        float2 crabC=float2(-.2,.12);float crab=disc(p,crabC,.036)+disc(p,crabC+float2(-.048,-.006),.022)+disc(p,crabC+float2(.048,-.006),.022);for(int j=0;j<4;j++){float y=.08+float(j)*.025;crab+=segment(p,crabC+float2(-.02,0),float2(-.29,y),.006)+segment(p,crabC+float2(.02,0),float2(-.11,y),.006);}
        float anemone=disc(p,float2(.2,.08),.03);for(int k=0;k<14;k++){float a=float(k)*.449+.08*sin(t*.3+float(k));anemone+=segment(p,float2(.2,.08),float2(.2,.08)+float2(cos(a),sin(a))*.075,.006);}
        c=mix(uSceneColor3,uSceneColor0,rim);c=mix(c,uSceneColor1,poolShape);c=mix(c,uSceneColor2,clamp(stones*.3+pow(abs(ripple(q)),5.0)*poolShape*.45,0.0,1.0));c=mix(c,uSceneColor3,shells);c=mix(c,uSceneColor2,clamp(crab+anemone,0.0,1.0));
    }else if(s==4){
        float horizon=.03,sky=1.0-smoothstep(horizon-.01,horizon+.01,p.y),sea=smoothstep(horizon-.01,horizon+.01,p.y)*smoothstep(.3,.22,p.y),sand=smoothstep(.21,.32,p.y);float sun=disc(p,float2(.3,-.25),.065);float foam=exp(-abs(p.y-.24-.02*sin(p.x*8.0-t*.35))*42.0)*(.35+.65*noise(float2(p.x*45.0,t)));
        float trunk=0.0,crown=0.0;float2 root=float2(-.49,.45),joint=root;for(int i=0;i<11;i++){float u=(float(i)+1.0)/11.0;float2 next=float2(-.49+.17*u+.035*sin(u*2.6),.45-.76*u);trunk+=segment(p,joint,next,.027-.013*u);joint=next;}
        for(int j=0;j<13;j++){float fj=float(j),a=-3.05+fj*.255+.045*sin(t*.2+fj);float len=.24+.09*hash(float2(fj,4.0));float2 tip=joint+float2(cos(a),sin(a))*len;crown+=segment(p,joint,tip,.013);for(int k=1;k<8;k++){float u=float(k)/8.0;float2 lc=mix(joint,tip,u),n=normalize(float2(-(tip-joint).y,(tip-joint).x));crown+=segment(p,lc,lc+n*(.028+.025*u)*sign(sin(float(k+j))),.005);}}
        float hut=box(p,float2(.35,.24),float2(.12,.08));float roof=smoothstep(.012,0.0,sdBox(rot(p-float2(.35,.14),.785),float2(.105,.105)))*step(.04,p.y);float chair=segment(p,float2(.08,.32),float2(.18,.25),.012)+segment(p,float2(.18,.25),float2(.24,.33),.012)+segment(p,float2(.12,.3),float2(.1,.4),.008);
        c=mix(uSceneColor3,uSceneColor2,clamp((p.y+.5)*1.2,0.0,1.0));c=mix(c,uSceneColor1,sea);c=mix(c,uSceneColor3,sand+sun+foam);c=mix(c,uSceneColor0,clamp(trunk+crown+hut+roof+chair,0.0,1.0));
    }else if(s==5){
        float swell=.5+.5*waveSum(float2(p.x*1.3,p.y*3.0),t*.45),water=.28+.16*swell;float breakers=0.0,glow=0.0;
        for(int i=0;i<5;i++){float fi=float(i),y=-.31+fi*.17+.035*sin(p.x*(3.0+fi)-t*(.35+.08*fi));float crest=exp(-abs(p.y-y)*(48.0-fi*4.0))*(.3+.7*noise(float2(p.x*52.0+fi,t*1.2)));breakers+=crest;glow+=crest*(.35+pow(noise(float2(p.x*24.0-fi,t*.4)),6.0));}
        float plankton=pow(noise((p+touchMemoryFlow(q)*.2)*32.0+float2(t*.12,0)),9.0)*(1.0+touchMemory(q));
        c=mix(uSceneColor0,uSceneColor1,water);c=mix(c,uSceneColor2,clamp(glow*.7+plankton,0.0,1.0));c=mix(c,uSceneColor3,clamp(breakers*.32+glow*.5,0.0,1.0));
    }else if(s==6){
        float depth=smoothstep(-.5,.5,p.y),rays=0.0;for(int r=0;r<5;r++){float fr=float(r),x=-.5+fr*.25;rays+=smoothstep(.09,0.0,abs(p.x-x-p.y*(.13-.025*fr)))*smoothstep(.48,-.5,p.y);}
        float backKelp=0.0,frontKelp=0.0;for(int i=0;i<15;i++){float fi=float(i),x=-.7+fi*.1,sway=.025*sin(t*.22+fi+p.y*5.0)+touchMemoryFlow(q).x*.025;float stalk=exp(-abs(p.x-x-sway)*65.0)*smoothstep(.48,-.43,p.y);float leaves=0.0;for(int j=0;j<5;j++){float y=.34-float(j)*.17;leaves+=disc(p,float2(x+sway+.04*sign(sin(float(i+j))),y),.035+.008*mod(float(j),2.0));}if(mod(fi,2.0)<1.0)backKelp+=stalk+leaves;else frontKelp+=stalk+leaves;}
        float fishSchool=0.0;for(int k=0;k<13;k++){float fk=float(k),x=-.65+fract(t*.02+fk*.09)*1.3,y=-.28+.045*sin(fk*2.0+t*.25);fishSchool+=fish(p,float2(x,y),.014+.004*mod(fk,3.0),1.0);}
        c=mix(uSceneColor0,uSceneColor1,.25+depth*.35);c=mix(c,uSceneColor3,rays*.28);c=mix(c,uSceneColor1,backKelp*.58);c=mix(c,uSceneColor2,clamp(frontKelp*.8+fishSchool,0.0,1.0));
    }else if(s==7){
        float sky=1.0-smoothstep(-.08,-.02,p.y),backDune=step(-.03+.1*sin(p.x*1.5),p.y),midDune=step(.12+.13*sin(p.x*1.8+.8),p.y),frontDune=step(.27+.1*sin(p.x*2.6-.4),p.y);float ridges=pow(.5+.5*sin((p.x+fbm(p*3.0)*.07)*85.0),20.0)*frontDune;
        float grass=0.0;for(int i=0;i<20;i++){float fi=float(i),x=-.68+fi*.072,y=.16+.12*sin(x*1.8+.8);grass+=segment(p,float2(x,y),float2(x+.018*sin(fi),y-.08-.04*hash(float2(fi,2.0))),.004);}
        float boardwalk=segment(p,float2(-.6,.37),float2(.55,.12),.025);float slats=0.0;for(int j=0;j<12;j++){float u=float(j)/11.0,cx=-.6+u*1.15,cy=.37-u*.25;slats+=segment(p,float2(cx-.025,cy-.02),float2(cx+.025,cy+.02),.006);}
        c=mix(uSceneColor3,uSceneColor2,sky*.22);c=mix(c,uSceneColor1,backDune*.4);c=mix(c,uSceneColor2,midDune*.58);c=mix(c,uSceneColor3,frontDune*.72);c=mix(c,uSceneColor1,ridges*.35+grass*.8);c=mix(c,uSceneColor0,boardwalk+slats);
    }else if(s==8){
        float horizon=-.03,sea=smoothstep(horizon-.01,horizon+.01,p.y);float clouds=fbm(float2(p.x*1.7,p.y*3.2+t*.035));float stormCloud=smoothstep(.35,.72,clouds)*(1.0-sea);float swell=waveSum(float2(p.x*1.2,p.y*2.7),t*.95),cross=waveSum(float2(-p.x*2.0,p.y*5.0),t*1.35);float water=.34+.2*swell+.08*cross;float foam=pow(max(0.0,swell+.38*cross),6.0)*sea;
        float rain=step(.965,hash(floor((q+float2(t*.45,-t*1.8))*float2(95.0,45.0))))*(1.0-sea);float flashWindow=step(.988,hash(float2(floor(t*.38),7.0)));float bolt=segment(p,float2(.2,-.46),float2(.13,-.27),.008)+segment(p,float2(.13,-.27),float2(.19,-.12),.007)+segment(p,float2(.19,-.12),float2(.11,.02),.006);float lightning=bolt*flashWindow;
        c=mix(uSceneColor1,uSceneColor0,clouds*.5);c=mix(c,mix(uSceneColor1,uSceneColor2,water),sea);c=mix(c,uSceneColor3,clamp(foam*.6+rain*.45+lightning,0.0,1.0));c+=uSceneColor3*flashWindow*.12*(1.0-sea);
    }else{
        float depth=smoothstep(-.5,.5,p.y),surface=line(p.y+.46,.025),caustic=pow(abs(sin(fbm(p*9.0+float2(t*.035,0))*20.0)),15.0);float rays=0.0;for(int i=0;i<7;i++){float fi=float(i),x=-.62+fi*.2;rays+=smoothstep(.07+.015*fi,0.0,abs(p.x-x-p.y*(.18-.03*fi)))*smoothstep(.48,-.44,p.y);}
        float arch=disc(p,float2(-.34,.23),.29)-disc(p,float2(-.34,.18),.18);arch*=step(-.5,p.x)*step(p.x,-.06);float rocks=0.0;for(int j=0;j<10;j++){float fj=float(j);rocks+=disc(p,float2(-.65+fj*.15,.43-.03*mod(fj,3.0)),.055+.025*hash(float2(fj,5.0)));}
        float2 diver=float2(.18+.06*sin(t*.12),-.02+.025*sin(t*.3));float diverBody=segment(p,diver,diver+float2(.12,.035),.018)+disc(p,diver-float2(.025,.005),.025)+segment(p,diver+float2(.12,.035),diver+float2(.2,.0),.009)+segment(p,diver+float2(.12,.035),diver+float2(.2,.08),.009);float bubbles=0.0;for(int b=0;b<6;b++){float fb=float(b),y=diver.y-.05-mod(t*.04+fb*.08,.45);bubbles+=line(length(p-float2(diver.x-.05+.02*sin(fb),y))-.008-.003*mod(fb,2.0),.003);}
        float school=0.0;for(int k=0;k<8;k++){float fk=float(k);school+=fish(p,float2(-.1+fract(t*.018+fk*.12)*.75,-.25+.04*sin(fk)),.017,1.0);}
        c=mix(uSceneColor1,uSceneColor0,depth*.55);c=mix(c,uSceneColor3,clamp(surface+rays*.28+caustic*.16+bubbles,0.0,1.0));c=mix(c,uSceneColor0,arch+rocks);c=mix(c,uSceneColor2,clamp(diverBody+school,0.0,1.0));
    }
    return half3(clamp(c,0.0,1.0));
}

float abstractField(float2 uv,int s){float2 p=(touchWarp(uv)-.5)*float2(uResolution.x/uResolution.y,1.0);float t=uTime;
    if(s==0){float2 scramble=touchMemoryFlow(uv)*(.7+.8*touchMemory(uv));float d=voronoi(p*7.0+scramble*3.0+fbm(p*2.0));return smoothstep(.6,.08,d);}
    if(s==1){float2 warped=p+touchMemoryFlow(uv)*1.8;float d=voronoi(warped*5.0);return line(d-.32,.025)+.35*voronoi(warped*12.0);}
    if(s==2){float n=fbm(float2(p.x*2.2+t*.06,p.y*7.0+fbm(p*3.0)));return .5+.5*sin(n*15.0+p.y*5.0);}
    if(s==3){float2 hp=p*7.0,r=float2(1.0,1.732),h=r*.5;float2 a=mod(hp,r)-h,b=mod(hp-h,r)-h;float2 gv=dot(a,a)<dot(b,b)?a:b;float d=max(abs(gv.x)*.866+abs(gv.y)*.5,abs(gv.y));return line(d-.43,.055);}
    if(s==4){float2 mp=p+touchMemoryFlow(uv)*1.4;float n=fbm(mp*3.0+float2(fbm(mp*2.0+t*.03),fbm(mp*2.0-t*.025)));return pow(abs(sin(n*17.0)),5.0);}
    if(s==5){float n=fbm(p*3.4+float2(t*.025,0));return line(fract(n*7.0)-.5,.08);}
    if(s==6){float2 rp=rot(p,.785)*9.0,id=floor(rp),g=fract(rp)-.5;float over=mod(id.x+id.y,2.0);float ribbonA=line(g.x+.16*sin(g.y*3.14159),.105),ribbonB=line(g.y-.16*sin(g.x*3.14159),.105);float stitch=line(abs(g.x)-.31,.035)*line(abs(g.y)-.31,.18);return (over<.5?ribbonA:ribbonB)+stitch*.65;}
    if(s==7){float a=atan(p.y,p.x),r=length(p);float metric=mix(r,max(abs(p.x),abs(p.y)),.5+.5*sin(t*.12));return line(fract(metric*5.0-a*.32)-.5,.08);}
    if(s==8){float2 fp=(touchFocus(uv)-.5)*float2(uResolution.x/uResolution.y,1.0);float a=sin((fp.x+fp.y)*30.0+fbm(fp*4.0)*5.0);float b=sin((fp.x-fp.y)*31.3-t*.15);return .5+.5*a*b;}
    float2 lp=p*3.1;float gy=sin(lp.x*2.4+t*.22)+sin(lp.y*3.1-t*.17)+sin((lp.x+lp.y)*1.7);float petals=cos(atan(lp.y,lp.x)*6.0+length(lp)*8.0-t*.3);float membrane=.5+.5*sin(gy*2.2+petals*1.4);return smoothstep(.28,.82,membrane);
}

float glyph(float2 local,float symbol,float row){return glyphAtlas.eval(float2((symbol+.5+local.x)*32.0,(row+.5+local.y)*32.0)).r;}
float rainLayer(float2 uv,float t,float row,float columns,float reverse){float rows=31.0;float2 grid=uv*float2(columns,rows),id=floor(grid),local=fract(grid)-.5;float speed=.12+hash(float2(id.x,row+4.0))*.32;float head=fract(t*speed+hash(float2(id.x,row+9.0)));if(reverse>.5)head=1.0-head;float delta=mod(head-uv.y+1.0,1.0);float trail=exp(-delta*(6.0+row));float leader=exp(-delta*55.0);float symbol=floor(hash(float2(id.x,id.y+floor(t*speed*rows)))*16.0);return glyph(local,symbol,row)*(trail+leader*.8);}
float techField(float2 uv,int s){float t=uTime;float r=0.0;
    if(s==0)r=rainLayer(uv,t,0.0,18.0,0.0)+.25*rainLayer(uv*float2(.82,1.0)+.09,-t*.55,0.0,15.0,0.0);
    else if(s==1)r=rainLayer(uv,t,1.0,23.0,0.0);
    else if(s==2)r=rainLayer(uv,t,2.0,20.0,0.0);
    else if(s==3)r=rainLayer(uv,t*.55,0.0,16.0,0.0)+.35*rainLayer(float2(1.0-uv.x,uv.y),t*.3,0.0,11.0,0.0);
    else if(s==4)r=rainLayer(float2(uv.x,1.0-uv.y),t*.85,0.0,19.0,1.0)+.3*rainLayer(uv,-t*.4,0.0,12.0,0.0);
    else if(s==5){float2 q=abs(uv-.5)*2.0;r=rainLayer(float2(q.x,q.y),t,0.0,16.0,0.0)*(1.0-q.x*.25);}
    else if(s==6){float2 q=fract(rot(uv-.5,.785)*2.0);r=rainLayer(q,t*.65,2.0,14.0,0.0);}
    else if(s==7){float bands=sin(uv.y*95.0+t*2.0)*.5+.5;r=bands*rainLayer(float2(uv.y,uv.x),t*.45,2.0,11.0,0.0);}
    else if(s==8){float base=rainLayer(uv,t*.7,0.0,17.0,0.0);r=base+pow(base,4.0)*1.7;}
    else {float intro=smoothstep(.85,.12,length(uv-.5)-fract(t*.06)*.8);r=rainLayer(uv,t,3.0,18.0,0.0)*intro;}
    return r+pow(abs(ripple(uv)),8.0)*.18;
}

half3 paletteField(float v){v=clamp(v,0.0,1.0);if(v<.333)return half3(mix(uSceneColor0,uSceneColor1,v*3.0));if(v<.666)return half3(mix(uSceneColor1,uSceneColor2,(v-.333)*3.0));return half3(mix(uSceneColor2,uSceneColor3,(v-.666)*3.0));}
half3 baseScene(float2 uv,int theme,int s){if(theme==0)return spaceScene(uv,s);if(theme==3)return arcadeScene(uv,s);if(theme==4)return beachScene(uv,s);float v=theme==1?abstractField(uv,s):techField(uv,s);return paletteField(v);}

half4 main(float2 fragCoord){float2 uv=fragCoord/uResolution;int filter=int(uFilter+.5);
    if(filter==2){float2 c=uv-.5;float r=dot(c,c);uv=.5+c*(1.0+r*.18);}else if(filter==3){float band=floor(fragCoord.y/18.0);uv.x+=(hash(float2(band,floor(uTime*8.0)))-.5)*step(.82,hash(float2(band,floor(uTime*8.0)+3.0)))*.065;}else if(filter==4)uv=(floor(fragCoord/10.0)*10.0+5.0)/uResolution;else if(filter==8){float2 p=uv-.5;float r2=dot(p,p);uv=.5+p*(1.0+r2*1.15+r2*r2*.9);}else if(filter==15){float2 p=uv-.5;uv+=(hash2(floor(p*24.0+uTime*.8))-.5)*.018;}
    int theme=int(uTheme+.5),s=int(uSubtheme+.5);half3 color=baseScene(uv,theme,s);float intensity=dot(color,half3(.2126,.7152,.0722));
    if(filter==1){float2 d=(uv-.5)/max(length(uv-.5),.001);float amount=.006+length(uv-.5)*.018;half3 a=baseScene(uv+d*amount,theme,s),b=baseScene(uv-d*amount,theme,s);color=half3(a.r,color.g,b.b);}
    else if(filter==2){float scan=.78+.22*sin(fragCoord.y*3.14159);float stripe=mod(floor(fragCoord.x),3.0);half3 mask=stripe<1.0?half3(1.0,.64,.6):(stripe<2.0?half3(.6,1.0,.64):half3(.64,.6,1.0));float2 edge=uv*(1.0-uv);color*=mask*half(scan*pow(clamp(edge.x*edge.y*18.0,0.0,1.0),.28));}
    else if(filter==3){float grain=hash(fragCoord+floor(uTime*24.0))-.5;color=color*half(.86+grain*.18)+half3(.04,-.01,.05);}
    else if(filter==5)color=color*.68+color*half(smoothstep(.14,.8,intensity)*.6)+half3(pow(intensity,5.0)*.12);
    else if(filter==6){half l=dot(color,half3(.299,.587,.114));color=half3(smoothstep(.16,.82,l));}
    else if(filter==7){float2 p=uv-.5;float a=atan(p.y,p.x),r=length(p);a=abs(fract(a/6.283185*8.0+.5)-.5)*6.283185/8.0;color=baseScene(.5+float2(cos(a),sin(a))*r,theme,s);}
    else if(filter==9){float2 cell=fract(fragCoord/6.0)-.5;float m=1.0-smoothstep(sqrt(max(intensity,0.0))*.48,sqrt(max(intensity,0.0))*.48+.08,length(cell));color=mix(half3(.015),color,half(m));}
    else if(filter==10){float v=clamp(intensity,0.0,1.0);color=v<.5?mix(half3(.025,0,.22),half3(.9,.04,.02),half(v*2.0)):mix(half3(.9,.04,.02),half3(1,.88,.08),half((v-.5)*2.0));}
    else if(filter==11)color=half3(1.0)-color;else if(filter==12)color=floor(color*half(5.0)+half(.5))/half(5.0);else if(filter==13){float grain=hash(fragCoord+floor(uTime*20.0))-.5;float v=1.0-smoothstep(.3,.75,length(uv-.5));color=pow(max(color,half3(0)),half3(.92))*half(.7+.3*v)+half3(.04,.015,-.01)+half3(grain*.1);}else if(filter==16){half l=dot(color,half3(.22,.72,.06));color=half3(.025,l*1.3+.1,.05);}
    return half4(clamp(color,half3(0.0),half3(1.0)),1.0);
}
"""

private data class DeviceMotion(val tiltX:Float=0f,val tiltY:Float=0f,val angularX:Float=0f,val angularY:Float=0f)

@Composable private fun rememberDeviceMotion():DeviceMotion{val context=LocalContext.current;var tx by remember{mutableFloatStateOf(0f)};var ty by remember{mutableFloatStateOf(0f)};var ax by remember{mutableFloatStateOf(0f)};var ay by remember{mutableFloatStateOf(0f)};DisposableEffect(context){val manager=context.getSystemService(Context.SENSOR_SERVICE) as SensorManager;val gravity=manager.getDefaultSensor(Sensor.TYPE_GRAVITY)?:manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);val gyro=manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);val listener=object:SensorEventListener{override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int)=Unit;override fun onSensorChanged(event:SensorEvent){when(event.sensor.type){Sensor.TYPE_GRAVITY,Sensor.TYPE_ACCELEROMETER->{tx+=((-event.values[0]/SensorManager.GRAVITY_EARTH).coerceIn(-1f,1f)-tx)*.12f;ty+=((event.values[1]/SensorManager.GRAVITY_EARTH).coerceIn(-1f,1f)-ty)*.12f};Sensor.TYPE_GYROSCOPE->{ax+=(event.values[0].coerceIn(-5f,5f)-ax)*.18f;ay+=(event.values[1].coerceIn(-5f,5f)-ay)*.18f}}}};gravity?.let{manager.registerListener(listener,it,SensorManager.SENSOR_DELAY_GAME)};gyro?.let{manager.registerListener(listener,it,SensorManager.SENSOR_DELAY_GAME)};onDispose{manager.unregisterListener(listener)}};return DeviceMotion(tx,ty,ax,ay)}

private fun createGlyphAtlas():Bitmap{val cell=32;val rows=listOf("アイウエオカキクケコサシスセソン","0101101001010110","0123456789ABCDEF","SYSTEMROOTACCESS");return Bitmap.createBitmap(cell*16,cell*rows.size,Bitmap.Config.ARGB_8888).also{bitmap->val canvas=AndroidCanvas(bitmap);canvas.drawColor(AndroidColor.BLACK);val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=AndroidColor.WHITE;textSize=24f;textAlign=Paint.Align.CENTER;typeface=Typeface.create(Typeface.MONOSPACE,Typeface.BOLD)};rows.forEachIndexed{r,chars->chars.take(16).forEachIndexed{c,ch->canvas.drawText(ch.toString(),c*cell+cell*.5f,r*cell+cell*.76f,paint)}}}}
internal fun abstractShaderSourceFor(theme:AbstractShaderTheme)=SOURCED_SHADER

@Composable internal fun AbstractShaderRenderer(theme:AbstractShaderTheme,subthemeIndex:Int,recolor:ShaderRecolor,customColors:List<Long>,touchPoints:List<TouchPoint>,animationSpeed:Float,filter:ThemeFilter,modifier:Modifier){if(Build.VERSION.SDK_INT<Build.VERSION_CODES.TIRAMISU){AbstractFallback(theme,subthemeIndex,recolor,touchPoints,animationSpeed,modifier);return};AbstractAgsl(theme,subthemeIndex,recolor,customColors,touchPoints,animationSpeed,filter,modifier)}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun AbstractAgsl(
    theme: AbstractShaderTheme,
    subthemeIndex: Int,
    recolor: ShaderRecolor,
    customColors: List<Long>,
    touchPoints: List<TouchPoint>,
    animationSpeed: Float,
    filter: ThemeFilter,
    modifier: Modifier
) {
    val motion = rememberDeviceMotion()
    val sceneIndex = subthemeIndex.coerceIn(0, 9)

    // Building the RuntimeShader and its glyph atlas is expensive, and the GPU program is
    // compiled and linked on the render thread at the first draw that uses it. On a cold shader
    // cache — which is exactly the state of a freshly installed app — that link blocks the main
    // thread inside syncAndDrawFrame for long enough to trip the 10 s ANR watchdog, so the very
    // first launch after install can present a black screen and an "isn't responding" dialog.
    //
    // Draw the cheap fallback for the first couple of frames instead. The window is then up,
    // focused and dispatching input before any of that work starts, so a slow first compile
    // costs a brief hitch rather than an unresponsive app.
    var shaderReady by remember(theme) { mutableStateOf(false) }
    LaunchedEffect(theme) {
        withFrameNanos { }
        withFrameNanos { }
        delay(120)
        shaderReady = true
    }

    val runtime = remember(theme, shaderReady) {
        if (!shaderReady) null else runCatching {
            RuntimeShader(SOURCED_SHADER).apply {
                val atlas = createGlyphAtlas()
                setInputShader("glyphAtlas", BitmapShader(atlas, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
            }
        }.onFailure { Log.e("MiniMateShader", "${theme.label} source shader compilation failed", it) }.getOrNull()
    }
    if (runtime == null) {
        AbstractFallback(theme, sceneIndex, recolor, touchPoints, animationSpeed, modifier)
        return
    }
    val transition = rememberInfiniteTransition(label = "SourcedThemeTime")
    val time by transition.animateFloat(0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "SourcedTime")
    Canvas(modifier.fillMaxSize()) {
        val points = touchPoints.takeLast(8)
        val positions = FloatArray(16) { -10f }
        val starts = FloatArray(8)
        val active = FloatArray(8)
        points.forEachIndexed { index, point ->
            positions[index * 2] = point.x / size.width
            positions[index * 2 + 1] = point.y / size.height
            starts[index] = point.startedAtSeconds
            active[index] = if (point.active) 1f else 0f
        }
        val scene = subthemesFor(theme)[sceneIndex]
        val colorwayIndex = if (recolor == ShaderRecolor.CUSTOM) 0 else recolor.ordinal.coerceIn(scene.colorways.indices)
        val stops = if (recolor == ShaderRecolor.CUSTOM && customColors.size == 4) customColors else scene.colorways[colorwayIndex].stops
        fun rgb(color: Long) = floatArrayOf(
            ((color shr 16) and 255).toFloat() / 255f,
            ((color shr 8) and 255).toFloat() / 255f,
            (color and 255).toFloat() / 255f
        )
        runtime.setFloatUniform("uResolution", size.width, size.height)
        runtime.setFloatUniform("uTime", time * animationSpeed)
        runtime.setFloatUniform("uNow", SystemClock.elapsedRealtime() / 1000f)
        runtime.setFloatUniform("uTheme", theme.ordinal.toFloat())
        runtime.setFloatUniform("uSubtheme", sceneIndex.toFloat())
        runtime.setFloatUniform("uVariant", colorwayIndex.toFloat())
        runtime.setFloatUniform("uPalette", recolor.ordinal.toFloat())
        runtime.setFloatUniform("uReaction", scene.reaction.ordinal.toFloat())
        List(4) { stops.getOrElse(it) { stops.last() } }.forEachIndexed { index, color ->
            runtime.setFloatUniform("uSceneColor$index", rgb(color))
        }
        runtime.setFloatUniform("uFilter", filter.ordinal.toFloat())
        runtime.setFloatUniform("uTouches", positions)
        runtime.setFloatUniform("uTouchStarts", starts)
        runtime.setFloatUniform("uTouchActive", active)
        runtime.setFloatUniform("uTouchCount", points.size.toFloat())
        runtime.setFloatUniform("uTilt", motion.tiltX, motion.tiltY)
        runtime.setFloatUniform("uAngularVelocity", motion.angularX, motion.angularY)
        drawRect(ShaderBrush(runtime))
    }
}

@Composable private fun AbstractFallback(theme:AbstractShaderTheme,subthemeIndex:Int,recolor:ShaderRecolor,touchPoints:List<TouchPoint>,animationSpeed:Float,modifier:Modifier){val touch=touchPoints.lastOrNull{it.active};val legacy=when(theme){AbstractShaderTheme.COSMIC->BackgroundTheme.NEBULA_SMOKE;AbstractShaderTheme.PRISMATIC->BackgroundTheme.PRISM_OIL;AbstractShaderTheme.TECH->BackgroundTheme.CYAN_VAPOR;AbstractShaderTheme.ARCADE->BackgroundTheme.PIXEL_ARCADE;AbstractShaderTheme.OCEANIC->BackgroundTheme.ABYSSAL_FLUID};LegacyGpuBackground(legacy,(subthemeIndex+recolor.ordinal)%3,animationSpeed,touch?.x?:0f,touch?.y?:0f,if(touch==null)0f else 1f,0f,modifier)}
