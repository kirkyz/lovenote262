package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Vfx;
import com.lovenote.util.Tex;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import org.joml.Matrix4f;

/** Small original animated 3D crown, halo or horns above the player. */
public final class HeadCosmetic extends Module {
 private final ModeSetting style=add(new ModeSetting("Style","Halo","Halo","Crown","Horns"));
 private final ModeSetting colour=add(new ModeSetting("Colour","Theme","Theme","Gold","Ice","Rainbow"));
 private final NumberSetting size=add(new NumberSetting("Size",.42,.2,1,.05));
 private final NumberSetting speed=add(new NumberSetting("Rotate",.6,0,3,.05));
 private final NumberSetting opacity=add(new NumberSetting("Opacity",210,20,255,5));
 private final BoolSetting thirdPerson=add(new BoolSetting("Third person only",true));
 public HeadCosmetic(){super("HeadCosmetic",Category.VISUALS);describe("Animated crown, horns or halo attached to your own head in third person.");}
 @Override public void onRender3D(WorldRenderContext ctx,PoseStack stack){
   if(mc.player==null||mc.level==null)return;
   if(thirdPerson.get()&&mc.options.getCameraType().isFirstPerson())return;
   var cam=ctx.camera().position();Matrix4f m=stack.last().pose();MeshBuilder b=Fx.colorQuadsDepth();
   double x=mc.player.getX(),y=mc.player.getY()+mc.player.getBbHeight()+.23,z=mc.player.getZ();
   double r=size.get(),t=System.nanoTime()*1e-9*speed.get();
   int c=Colors.alpha(switch(colour.get()){case "Gold"->0xFFFFD27D;case "Ice"->0xFF7FE6FF;case "Rainbow"->Colors.rainbow(0);default->Colors.wave(0);},opacity.getI());
   if(style.is("Horns")){Vfx.tube(b,m,x-r,y-.15,z,x-r*.8,y+r*.9,z,.029f,c,c,cam.x,cam.y,cam.z);
       Vfx.tube(b,m,x+r,y-.15,z,x+r*.8,y+r*.9,z,.029f,c,c,cam.x,cam.y,cam.z);}
   else {for(int i=0;i<24;i++){
      double a=t+i*Math.PI/12,q=t+(i+1)*Math.PI/12;
      double y0=y+(style.is("Crown")?(i%4==0?.23:.0):.05*Math.sin(t*2+i*.5));
      double y1=y+(style.is("Crown")?((i+1)%4==0?.23:.0):.05*Math.sin(t*2+(i+1)*.5));
      Vfx.tube(b,m,x+Math.cos(a)*r,y0,z+Math.sin(a)*r,x+Math.cos(q)*r,y1,z+Math.sin(q)*r,.014f,c,c,cam.x,cam.y,cam.z);
   }}
   Fx.draw();
 }
}
