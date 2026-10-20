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

/** Original, lightweight animated wireframe companion; not a copied Anubis GLTF character. */
public final class Character extends Module {
 private final ModeSetting style=add(new ModeSetting("Companion","Spectre","Spectre","Guardian","Pixel Droid"));
 private final ModeSetting colour=add(new ModeSetting("Colour","Theme","Theme","Gold","Ice","Rose","Rainbow"));
 private final NumberSetting scale=add(new NumberSetting("Height",1.2,.5,2.4,.1));
 private final NumberSetting distance=add(new NumberSetting("Follow offset",2.8,1,6,.2));
 private final NumberSetting opacity=add(new NumberSetting("Opacity",150,20,250,5));
 private final NumberSetting speed=add(new NumberSetting("Animation speed",1,0,3,.1));
 private final BoolSetting throughBlocks=add(new BoolSetting("Show through blocks (test)",true));
 public Character(){super("Character",Category.VISUALS);describe("An animated hovering 3D wireframe companion with adjustable pose, scale and colour.");}
 @Override public void onRender3D(WorldRenderContext ctx,PoseStack stack){
   if(mc.player==null||mc.level==null)return;
   var cam=ctx.camera().position();Matrix4f m=stack.last().pose();MeshBuilder b=throughBlocks.get()?Fx.colorQuads():Fx.colorQuadsDepth();
   double t=System.nanoTime()*1e-9*speed.get(),yaw=Math.toRadians(mc.player.getYRot());
   double d=distance.get();
   // In front and to the right in first person, rather than hidden behind the camera.
   double x=mc.player.getX()-Math.sin(yaw)*d+Math.cos(yaw)*1.15;
   double z=mc.player.getZ()+Math.cos(yaw)*d+Math.sin(yaw)*1.15;
   double k=scale.get(),y=mc.player.getY()+.4+Math.sin(t*1.5)*.14;
   int c=Colors.alpha(switch(colour.get()){
     case "Gold"->0xFFFFCA7A;case "Ice"->0xFF84EAFF;case "Rose"->0xFFFF78D5;case "Rainbow"->Colors.rainbow(0);default->Colors.wave(0);},opacity.getI());
   if(style.is("Pixel Droid")){
     Vfx.cube(b,m,new Vfx.P(x,y+1.25*k,z),.45*k,t*.15,0,0,.014f,c,cam.x,cam.y,cam.z);
     Vfx.cube(b,m,new Vfx.P(x,y+.60*k,z),.38*k,t*.15,0,0,.015f,c,cam.x,cam.y,cam.z);
   }else{
     Vfx.crystal(b,m,new Vfx.P(x,y+1.56*k,z),.31*k,.34*k,t*.28,0,0,.017f,c,c,cam.x,cam.y,cam.z);
     Vfx.tube(b,m,x,y+.55*k,z,x,y+1.28*k,z,.032f,c,c,cam.x,cam.y,cam.z);
     double wa=Math.sin(t*2.5)*.18*k;
     Vfx.tube(b,m,x,y+1.18*k,z,x-.54*k,y+(.68*k)+wa,z,.027f,c,c,cam.x,cam.y,cam.z);
     Vfx.tube(b,m,x,y+1.18*k,z,x+.54*k,y+(.68*k)-wa,z,.027f,c,c,cam.x,cam.y,cam.z);
     if(style.is("Guardian"))Vfx.cube(b,m,new Vfx.P(x,y+.82*k,z),.31*k,t*.3,0,0,.015f,c,cam.x,cam.y,cam.z);
   }
   Vfx.tube(b,m,x,y+.55*k,z,x-.30*k,y,z,.025f,c,c,cam.x,cam.y,cam.z);
   Vfx.tube(b,m,x,y+.55*k,z,x+.30*k,y,z,.025f,c,c,cam.x,cam.y,cam.z);
   Fx.draw();
 }
}
