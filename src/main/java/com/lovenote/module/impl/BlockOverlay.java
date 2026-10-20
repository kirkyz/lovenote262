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

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
/** Subtle animated translucent selection faces; complements Drainer's existing block outline. */
public final class BlockOverlay extends Module {
 private final ModeSetting colour=add(new ModeSetting("Colour","Theme","Theme","Rainbow","Cyan","Rose","White"));
 private final NumberSetting opacity=add(new NumberSetting("Face opacity",42,5,130,5));
 private final NumberSetting pulse=add(new NumberSetting("Pulse speed",1.4,0,5,.1));
 private final BoolSetting depth=add(new BoolSetting("Respect terrain",true));
 public BlockOverlay(){super("BlockOverlay",Category.VISUALS);describe("Pulsing translucent block selection with configurable fill and depth.");}
 @Override public void onRender3D(WorldRenderContext ctx,PoseStack stack){
  if(mc.level==null||mc.player==null||!(mc.hitResult instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK)return;
  var pos=hit.getBlockPos();if(mc.level.getBlockState(pos).isAir())return;
  Matrix4f m=stack.last().pose();double x=pos.getX(),y=pos.getY(),z=pos.getZ(),a=1.001;
  int c=switch(colour.get()){case "Rainbow"->Colors.rainbow(0);case "Cyan"->0xFF6DE5FF;
    case "Rose"->0xFFFF76CD;case "White"->0xFFFFFFFF;default->Colors.wave(0);};
  float opacityFactor=pulse.get()==0?1f:(float)(.70+.30*Math.sin(System.nanoTime()*1e-9*pulse.get()*2));
  c=Colors.alpha(c,(int)(opacity.get()*opacityFactor));MeshBuilder b=depth.get()?Fx.colorQuadsDepth():Fx.colorQuads();
  // Three visible-facing planes selected using the actual camera position.
  var eye=ctx.camera().position();
  if(eye.x>=x+.5)face(b,m,x+a,y,z,x+a,y+1,z,x+a,y+1,z+1,x+a,y,z+1,c);
  else face(b,m,x-(a-1),y,z,x-(a-1),y,z+1,x-(a-1),y+1,z+1,x-(a-1),y+1,z,c);
  if(eye.y>=y+.5)face(b,m,x,y+a,z,x,y+a,z+1,x+1,y+a,z+1,x+1,y+a,z,c);
  else face(b,m,x,y-(a-1),z,x+1,y-(a-1),z,x+1,y-(a-1),z+1,x,y-(a-1),z+1,c);
  if(eye.z>=z+.5)face(b,m,x,y,z+a,x+1,y,z+a,x+1,y+1,z+a,x,y+1,z+a,c);
  else face(b,m,x,y,z-(a-1),x,y+1,z-(a-1),x+1,y+1,z-(a-1),x+1,y,z-(a-1),c);
  Fx.draw();
 }
 private static void face(MeshBuilder b,Matrix4f m,double ax,double ay,double az,double bx,double by,double bz,double cx,double cy,double cz,double dx,double dy,double dz,int c){
   Fx.vc(b,m,(float)ax,(float)ay,(float)az,c);Fx.vc(b,m,(float)bx,(float)by,(float)bz,c);
   Fx.vc(b,m,(float)cx,(float)cy,(float)cz,c);Fx.vc(b,m,(float)dx,(float)dy,(float)dz,c);
 }
}
