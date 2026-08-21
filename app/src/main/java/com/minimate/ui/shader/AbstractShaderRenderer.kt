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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
half3 arcadeScene(float2 uv,int s){float2 p=(touchWarp(uv)-.5)*float2(uResolution.x/uResolution.y,1.0);p=floor(p*160.0)/160.0;float2 target=(latestTouch()-.5)*float2(uResolution.x/uResolution.y,1.0);float touched=step(.5,uTouchCount);float t=uTime,v=0.0,accent=0.0;
    if(s==0){float frame=box(p,float2(-.07,0),float2(.31,.48));float2 g=floor((p+float2(.34,.48))*float2(14.0,20.0));float board=step(0.0,g.x)*step(g.x,9.0)*step(0.0,g.y)*step(g.y,19.0);float height=3.0+floor(hash(float2(g.x,2.0))*5.0);float pile=step(19.0-height,g.y)*step(.2,hash(g+float2(3.0,7.0)));float fallY=2.0+mod(floor(t*2.1),13.0);float autoX=3.0+mod(floor(t*.37),4.0),touchX=clamp(floor((target.x+.34)*14.0),0.0,9.0),fallX=mix(autoX,touchX,touched);float piece=(step(abs(g.x-fallX),1.0)*step(abs(g.y-fallY),.1)+step(abs(g.x-fallX),.1)*step(abs(g.y-fallY),1.0))*board;float clear=step(.84,fract(t*.17))*step(17.5,g.y);float grid=(line(fract((p.x+.34)*14.0)-.5,.035)+line(fract((p.y+.48)*20.0)-.5,.035))*board;float nextBox=box(p,float2(.35,-.28),float2(.1,.11));v=frame*.1+board*.08+grid*.12+pile*.62+piece+clear*.7+nextBox*.3;accent=piece+clear;}
    else if(s==1){float bend=.15*sin(p.y*3.2+t*.22)+.05*sin(p.y*7.0-t*.13);float road=smoothstep(.44,.4,abs(p.x-bend));float edge=line(abs(p.x-bend)-.4,.024);float lane=line(p.x-bend,.014)*step(.52,fract((p.y+t*.32)*8.0));float grass=.1+.08*noise(float2(p.x*16.0,p.y*18.0+t*.2));float car1=box(p,float2(bend+.14,.2),float2(.06,.105))+box(p,float2(bend+.14,.17),float2(.042,.035));float car2=box(p,float2(-bend-.15,-.22),float2(.058,.1))+box(p,float2(-bend-.15,-.25),float2(.04,.032));float shadows=box(p,float2(bend+.15,.22),float2(.07,.11))*.15+box(p,float2(-bend-.14,-.2),float2(.068,.105))*.15;v=grass*(1.0-road)+road*.24+edge*.65+lane*.72+shadows+car1*.82+car2*.58;accent=car1;}
    else if(s==2){float pitch=box(p,float2(0),float2(.62,.46));float markings=line(abs(p.x)-.6,.009)+line(abs(p.y)-.44,.009)+line(p.x,.008)+line(length(p)-.12,.009);float goals=box(p,float2(-.61,0),float2(.035,.13))+box(p,float2(.61,0),float2(.035,.13));float2 ball=float2(.32*sin(t*.7),.26*sin(t*.93));float players=0.0;for(int i=0;i<10;i++){float fi=float(i),side=fi<5.0?-1.0:1.0;float2 home=float2(side*(.16+.09*mod(fi,3.0)),-.32+mod(fi,5.0)*.16);float2 c=mix(home,ball,.18+.06*hash(float2(fi)))+float2(.018*sin(t+fi),.018*cos(t*.8+fi));players+=disc(p,c,.03)+box(p,c+float2(0,.035),float2(.022,.028));}float net=(line(fract((p.y+.14)*35.0)-.5,.06))*(goals);v=pitch*.16+markings*.58+goals*.5+net*.35+players*.68+disc(p,ball,.022);accent=disc(p,ball,.04);}
    else if(s==3){float cabinet=box(p,float2(0),float2(.48,.47));float bricks=0.0;float2 g=floor((p+float2(.45,.4))*float2(11.0,15.0));if(g.y<6.0&&g.x>=0.0&&g.x<10.0){float hitTime=hash(g)*14.0;bricks=step(mod(t*.55,14.0),hitTime)*step(.1,fract((p.x+.45)*11.0))*step(.12,fract((p.y+.4)*15.0));}float triangle=abs(fract(t*.38)*2.0-1.0);float2 ball=float2(.4*sin(t*.83),-.25+.58*triangle);float paddleX=mix(.4*sin(t*.83),clamp(target.x,-.35,.35),touched);float paddle=box(p,float2(paddleX,.4),float2(.13,.022));float trail=segment(p,ball,ball-float2(.035*cos(t*.83),.045),.009);v=cabinet*.06+bricks*.58+paddle+trail*.35+disc(p,ball,.022);accent=disc(p,ball,.04);}
    else if(s==4){float cabinet=box(p,float2(0),float2(.58,.47));float march=floor(mod(t*.72,8.0))*.012-.042;float descend=floor(t*.09)*.018;float swarm=0.0;for(int y=0;y<4;y++)for(int x=0;x<7;x++){float2 c=float2(-.48+float(x)*.16,-.32+float(y)*.12+mod(descend,.1))+float2(march,0);float alive=step(.13,hash(float2(float(x+y*7),floor(t*.08))));swarm+=alien(p,c,.024)*alive;}float shields=0.0;for(int b=0;b<4;b++){float bx=-.42+float(b)*.28;float dome=disc(p,float2(bx,.24),.075);float cut=box(p,float2(bx,.29),float2(.025,.04));shields+=max(0.0,dome-cut)*(1.0-.38*step(.72,hash(float2(float(b),floor(t*.3)))));}float playerX=mix(.36*sin(t*.63),clamp(target.x,-.4,.4),touched),player=box(p,float2(playerX,.41),float2(.085,.022))+box(p,float2(playerX,.375),float2(.022,.04));float playerShot=box(p,float2(playerX,.32-mod(t*.37,.58)),float2(.006,.028));float alienShot=0.0;for(int k=0;k<3;k++){float fk=float(k),sx=-.31+fk*.3+march,sy=-.08+mod(t*(.19+.03*fk)+fk*.21,.5);alienShot+=box(p,float2(sx,sy),float2(.007,.025));}float blast=disc(p,float2(-.18+march,.02),.035+.018*sin(t*5.0))*step(.9,fract(t*.21));v=cabinet*.035+swarm*.72+shields*.42+player+playerShot+alienShot*.72+blast;accent=playerShot+blast;}
    else if(s==5){float2 grid=floor((p+float2(.5,.48))*float2(18.0,20.0));float border=step(grid.x,0.0)+step(17.0,grid.x)+step(grid.y,0.0)+step(19.0,grid.y);float vertical=step(mod(grid.x,4.0),.1)*step(2.0,mod(grid.y,6.0));float horizontal=step(mod(grid.y,5.0),.1)*step(2.0,mod(grid.x,7.0));float maze=clamp(border+vertical+horizontal,0.0,1.0);float dots=(1.0-maze)*step(.8,fract(grid.x*.37+grid.y*.61));float phase=fract(t*.075);float2 hero=phase<.5?float2(-.4+phase*1.6,-.29):float2(.4-(phase-.5)*1.6,.24);float2 ghostPos=float2(.32*sin(t*.42),.2*sin(t*.31));float pac=disc(p,hero,.048);float mouth=step(.78,dot(normalize(p-hero),float2(cos(t*3.0),sin(t*3.0))))*pac;float ghost=box(p,ghostPos+float2(0,.025),float2(.045,.05))+disc(p,ghostPos-float2(0,.025),.045);v=maze*.5+dots*.25+(pac-mouth)+ghost*.72;accent=cuteFace(p,ghostPos,.06);}
    else if(s==6){float river=step(-.04,p.y)*step(p.y,.31),road=step(-.43,p.y)*step(p.y,-.1),banks=step(.31,p.y)+step(p.y,-.43)+step(-.1,p.y)*step(p.y,-.04);float laneMarks=(line(p.y+.18,.008)+line(p.y+.34,.008))*road;float traffic=0.0;for(int i=0;i<5;i++){float fi=float(i),dir=mod(fi,2.0)<1.0?1.0:-1.0;float x=fract(t*(.09+.014*fi)+fi*.23)*1.5-.75;if(dir<0.0)x=-x;float y=-.39+fi*.068;float car=box(p,float2(x,y),float2(.085,.026))+box(p,float2(x-dir*.018,y),float2(.04,.035));traffic+=car;}float logs=0.0,turtles=0.0;for(int j=0;j<4;j++){float fj=float(j),dir=mod(fj,2.0)<1.0?1.0:-1.0;float x=fract(t*(.035+.007*fj)+fj*.29)*1.55-.78;if(dir<0.0)x=-x;float y=.0+fj*.085;logs+=box(p,float2(x,y),float2(.17,.026));logs+=disc(p,float2(x-dir*.15,y),.027);turtles+=disc(p,float2(-x,y),.03)+disc(p,float2(-x+.045,y),.03);}float hop=floor(mod(t*.82,9.0));float2 frog=float2(.15*sin(hop*1.7),.43-hop*.098);float body=disc(p,frog,.035)+box(p,frog,float2(.035,.04));float legs=segment(p,frog+float2(-.025,.02),frog+float2(-.065,.055),.011)+segment(p,frog+float2(.025,.02),frog+float2(.065,.055),.011);float wakes=(pow(abs(sin((p.x+t*.3)*45.0)),16.0)*river)*.16;v=banks*.18+road*.1+river*.24+laneMarks*.3+traffic*.74+logs*.52+turtles*.58+wakes+body+legs;accent=cuteFace(p,frog,.05);}
    else if(s==7){float board=box(p,float2(0),float2(.46));float2 g=floor((p+float2(.46))*16.0);float headX=mod(floor(t*4.0),13.0)+1.0,headY=7.0+floor(3.0*sin(t*.35));float snake=0.0;for(int i=0;i<11;i++){float fi=float(i),sx=mod(headX-fi+13.0,13.0)+1.0,sy=headY+floor(1.2*sin((headX-fi)*.55));snake+=step(length(g-float2(sx,sy)),.1);}float apple=step(length(g-float2(11.0,11.0)),.1);float gridLines=(line(fract((p.x+.46)*16.0)-.5,.025)+line(fract((p.y+.46)*16.0)-.5,.025))*board;v=board*.07+gridLines*.08+snake*.72+apple;accent=apple+step(length(g-float2(headX+1.0,headY)),.1);}
    else if(s==8){float table=box(p,float2(0),float2(.45,.47));float rail=line(abs(p.x)-.43,.015)+line(abs(p.y)-.45,.015);float2 ballPos=float2(.31*sin(t*.9),.34*cos(t*.73));float ball=disc(p,ballPos,.022);float bump=disc(p,float2(-.2,-.14),.075)+disc(p,float2(.18,-.05),.068)+disc(p,float2(0,.15),.06);float bumpRing=line(length(p-float2(-.2,-.14))-.09,.012)+line(length(p-float2(.18,-.05))-.083,.012);float lanes=line(abs(p.x-.31)-.055,.01)*step(p.y,-.04);float flippers=box(rot(p-float2(-.12,.33),-.35*sin(t*2.0)),float2(0),float2(.12,.022))+box(rot(p-float2(.12,.33),.35*sin(t*2.0)),float2(0),float2(.12,.022));v=table*.08+rail*.5+lanes*.35+bump*.55+bumpRing*.72+flippers+ball;accent=ball;}
    else {float scroll=t*.1;float groundY=.3+.035*sin((p.x+scroll)*8.0);float ground=step(groundY,p.y);float2 tile=floor(float2((p.x+scroll)*12.0,p.y*11.0));float blocks=step(.87,hash(tile))*step(.02,p.y)*step(p.y,.3);float coins=0.0;for(int i=0;i<6;i++){float fi=float(i),x=-.6+mod(fi*.23-scroll,1.35);coins+=disc(p,float2(x,.05+.08*sin(fi*1.7)),.022);}float enemyX=.42-mod(scroll*.8,1.0);float enemy=box(p,float2(enemyX,.25),float2(.045,.04))+cuteFace(p,float2(enemyX,.25),.045);float2 hero=float2(-.2,.22-.1*pow(max(0.0,sin(t*1.7)),2.0));float body=box(p,hero,float2(.045,.065));float head=box(p,hero-float2(0,.065),float2(.055,.045));v=ground*.28+blocks*.48+coins*.8+enemy*.62+body+head;accent=cuteFace(p,hero-float2(0,.065),.055);}
    half3 color=half3(pal3(clamp(v,0.0,1.0)));color+=half3(uSceneColor3)*half(accent*.35);color*=half(.94+.06*sin(uv.y*uResolution.y*1.5708));return color;
}

float waveSum(float2 p,float t){float w=0.0;w+=sin(dot(p,float2(.82,.57))*7.0+t*.72)*.48;w+=sin(dot(p,float2(-.38,.92))*11.0+t*.94)*.26;w+=sin(dot(p,float2(.96,.28))*17.0+t*1.21)*.14;w+=sin(dot(p,float2(-.7,.71))*23.0+t*1.47)*.08;return w;}
float fish(float2 p,float2 c,float z,float dir){float body=smoothstep(1.0,.82,length((p-c)*float2(1.0/z,1.8/z)));float2 tailP=p-(c-float2(dir*z*.9,0));float tail=smoothstep(.25,0.0,abs(tailP.y)-abs(tailP.x)*.7)*step(0.0,-tailP.x*dir)*step(abs(tailP.x),z*.55);return body+tail;}
half3 beachScene(float2 uv,int s){float2 q=touchWarp(uv)+touchMemoryFlow(uv)*.026,p=(q-.5)*float2(uResolution.x/uResolution.y,1.0);float t=uTime,v=0.0;
    if(s==0){float shore=.04+.075*sin(p.x*2.7+t*.16)+.025*noise(float2(p.x*10.0,t*.16));float water=smoothstep(shore-.12,shore+.1,p.y);float wet=smoothstep(shore-.34,shore-.11,p.y)*(1.0-water);float dryGrain=.08*noise(p*95.0);float wash1=exp(-abs(p.y-shore)*42.0),wash2=exp(-abs(p.y-shore+.11+.025*sin(p.x*5.0-t*.25))*55.0);float lace=(wash1+wash2)*(.28+.72*noise(float2(p.x*58.0,t*1.2)));float current=.5+.5*waveSum(float2(p.x*1.2,p.y*2.4),t*.35);float touchEddy=touchMemory(q)*.28;v=.12+dryGrain+wet*.16+water*(.42+.12*current)+lace*.5+touchEddy*water;}
    else if(s==1){float horizon=-.08;float skyMask=1.0-smoothstep(horizon-.008,horizon+.008,p.y);float sea=1.0-skyMask;float perspective=max(.06,p.y-horizon);float waves=waveSum(float2(p.x/(perspective+1.0),p.y*5.0),t);float crests=pow(max(0.0,waves),6.0)*sea;float sun=exp(-length(p-float2(.28,-.27))*16.0);float3 skyColor=mix(uSceneColor3,uSceneColor2,clamp((p.y+.5)*1.2,0.0,1.0));float3 waterColor=mix(uSceneColor0,uSceneColor1,.45+.22*waves);float3 c=mix(waterColor,skyColor,skyMask)+uSceneColor3*(crests*.32+sun*.55);return half3(clamp(c,0.0,1.0));}
    else if(s==2){float caustic=pow(abs(sin(fbm(p*8.0+float2(t*.05,0))*18.0)),13.0);float reefMounds=0.0,branches=0.0,polyps=0.0;for(int i=0;i<9;i++){float fi=float(i),x=-.62+fi*.155,baseY=.38-.04*hash(float2(fi,2.0));reefMounds+=disc(p,float2(x,baseY),.09+.035*hash(float2(fi,5.0)));for(int j=0;j<4;j++){float fj=float(j),a=-1.9+fj*.42+.18*sin(fi);float2 root=float2(x,baseY-.05),tip=root+float2(cos(a),sin(a))*(.12+.025*hash(float2(fi,fj)));branches+=segment(p,root,tip,.018);polyps+=disc(p,tip,.018);}}float fishes=0.0;for(int k=0;k<7;k++){float fk=float(k),dir=mod(fk,2.0)<1.0?1.0:-1.0;float2 c=float2(fract(t*(.025+.004*fk)+fk*.19)*1.45-.72,-.32+mod(fk,3.0)*.13);if(dir<0.0)c.x=-c.x;fishes+=fish(p,c,.028+.007*mod(fk,3.0),dir);}v=.12+caustic*.22+reefMounds*.35+branches*.6+polyps*.8+fishes*.72;}
    else if(s==3){float edge=min(min(q.x,1.0-q.x),min(q.y,1.0-q.y));float rock=smoothstep(.15+.045*noise(q*8.0),.03,edge);float stones=0.0,shells=0.0;for(int i=0;i<11;i++){float fi=float(i);float2 c=(hash2(float2(fi,7.0))-.5)*float2(1.08,.7);float rad=.025+.025*hash(float2(fi,4.0));stones+=disc(p,c,rad);shells+=line(length(p-c-float2(rad*.15,0))-rad*.55,.007)*step(.72,hash(float2(fi,11.0)));}float anemone=disc(p,float2(.18,.12),.045);for(int j=0;j<12;j++){float a=float(j)*.5236;anemone+=segment(p,float2(.18,.12),float2(.18,.12)+float2(cos(a),sin(a))*(.08+.015*sin(t*.35+float(j))),.009);}float crab=box(p,float2(-.2,.16),float2(.05,.025))+segment(p,float2(-.23,.16),float2(-.3,.11),.009)+segment(p,float2(-.17,.16),float2(-.1,.11),.009);v=.22+rock*.38+stones*.18+shells*.4+anemone*.62+crab*.7+pow(abs(ripple(q)),5.0)*.32;}
    else if(s==4){float shore=.17+.035*sin(p.x*4.0+t*.08);float sea=smoothstep(shore-.02,shore+.08,p.y);float sand=.35+.07*noise(p*75.0);float trunk=0.0,crown=0.0;float2 root=float2(-.47,.43),joint=root;for(int i=0;i<9;i++){float fi=float(i),u=(fi+1.0)/9.0;float2 next=float2(-.47+.12*u+.025*sin(u*2.2),.43-.72*u);trunk+=segment(p,joint,next,.032-.014*u);joint=next;}for(int j=0;j<11;j++){float fj=float(j),a=-2.95+fj*.29+.035*sin(t*.28+fj);float len=.28+.08*hash(float2(fj,4.0));float2 tip=joint+float2(cos(a),sin(a))*len;crown+=segment(p,joint,tip,.018);for(int k=1;k<7;k++){float u=float(k)/7.0;float2 c=mix(joint,tip,u),normal=normalize(float2(-(tip-joint).y,(tip-joint).x));crown+=segment(p,c,c+normal*(.035+.025*u)*sign(sin(float(k+j))),.008);}}float boatHull=segment(p,float2(.23,.02),float2(.48,.02),.035);float mast=segment(p,float2(.35,.02),float2(.35,-.15),.012);float sail=smoothstep(.018,0.0,sdBox(rot(p-float2(.39,-.08),-.3),float2(.06,.08)));v=sand*(1.0-sea)+sea*.3+trunk*.68+crown*.55+boatHull*.7+mast*.7+sail*.52;}
    else if(s==5){float phase=fract(t*.085),crest=-.08+.14*sin(p.x*2.4+t*.3);float face=smoothstep(crest+.38,crest-.25,p.y)*smoothstep(crest-.38,crest-.03,p.y);float curl=line(length((p-float2(.18,crest-.05))*float2(1.0,1.55))-.24,.04)*step(p.x,-.01);float foam=exp(-abs(p.y-crest)*34.0)*(.24+.76*noise(float2(p.x*38.0,t*.75)));float backwash=exp(-abs(p.y-crest-.23)*25.0)*noise(float2(p.x*28.0,-t*.5));float glow=pow(noise(p*30.0+float2(t*.28,0)),8.0)*(face+foam+backwash)+pow(abs(ripple(q)),6.0);v=.06+face*.27+curl*.42+foam*.42+backwash*.18+glow;}
    else if(s==6){float depth=smoothstep(-.5,.5,p.y),kelp=0.0;for(int i=0;i<11;i++){float fi=float(i),x=-.65+fi*.13;float sway=.035*sin(t*.25+fi+p.y*4.0);kelp+=exp(-abs(p.x-x-sway)*55.0)*smoothstep(.46,-.4,p.y);for(int j=0;j<4;j++){float y=.34-float(j)*.19;kelp+=disc(p,float2(x+sway+.04*sign(sin(float(j+i))),y),.045);}}float school=0.0;for(int k=0;k<8;k++){float fk=float(k);school+=fish(p,float2(-.5+fract(t*.025+fk*.13)*1.1,-.2+.035*sin(fk*2.0+t*.3)),.018+.004*mod(fk,3.0),1.0);}v=.12+depth*.18+kelp*.65+school*.55;}
    else if(s==7){float backDune=.02+.12*sin(p.x*1.4),frontDune=.18+.17*sin(p.x*1.8+.7)+.045*sin(p.x*5.0);float back=step(backDune,p.y),front=step(frontDune,p.y);float ridges=pow(.5+.5*sin((p.x+fbm(p*3.0)*.08)*72.0),18.0)*front;float grass=0.0;for(int i=0;i<13;i++){float fi=float(i),x=-.66+fi*.11;grass+=segment(p,float2(x,frontDune+.02),float2(x+.025*sin(fi),frontDune-.08-.04*hash(float2(fi))),.006);}float fence=segment(p,float2(-.58,.18),float2(.58,.12),.01);for(int j=0;j<6;j++){float x=-.55+float(j)*.22;fence+=segment(p,float2(x,.25),float2(x,.05),.009);}float grains=step(.992,hash(floor((q+float2(t*.026,0))*95.0)));v=back*.22+front*.38+ridges*.34+grass*.6+fence*.52+grains*.25;}
    else if(s==8){float clouds=.16+.58*fbm(float2(p.x*1.7,p.y*2.8+t*.025));float sea=step(.0,p.y),swell=waveSum(float2(p.x*1.3,p.y*3.0),t*.75);float chop=waveSum(float2(-p.x*2.1,p.y*5.5),t*1.2);float water=.28+.18*swell+.09*chop;float foam=pow(max(0.0,swell+.45*chop),7.0)*sea;float horizonFoam=exp(-abs(p.y-.03)*45.0)*noise(float2(p.x*40.0,t));float flash=step(.992,hash(float2(floor(t*.5),3.0)))*exp(-abs(p.x+.2)*45.0);v=clouds*(1.0-sea)+sea*water+foam*.48+horizonFoam*.25+flash;}
    else {float rays=0.0;for(int i=0;i<6;i++){float fi=float(i),x=-.62+fi*.25+.04*sin(t*.1+fi);rays+=smoothstep(.075,0.0,abs(p.x-x-p.y*(.15-.035*fi)))*smoothstep(.55,-.5,p.y);}float caustic=pow(abs(sin(fbm(p*9.0+float2(t*.03,0))*20.0)),16.0);float ruins=box(p,float2(-.28,.28),float2(.07,.22))+box(p,float2(.28,.3),float2(.065,.2))+segment(p,float2(-.38,.08),float2(.39,.12),.025);float rocks=0.0;for(int j=0;j<8;j++){float fj=float(j);rocks+=disc(p,float2(-.6+fj*.17,.43-.025*mod(fj,2.0)),.06+.018*hash(float2(fj)));}float school=0.0;for(int k=0;k<6;k++){float fk=float(k);school+=fish(p,float2(-.45+fract(t*.022+fk*.17)*.9,-.2+.05*sin(fk)),.022,1.0);}v=.1+rays*.38+caustic*.23+ruins*.48+rocks*.3+school*.55;}
    return half3(pal3(clamp(v,0.0,1.0)));
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
    val runtime = remember(theme) {
        runCatching {
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
