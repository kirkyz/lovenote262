package com.lovenote.module.impl;
import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.BoolSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Vfx;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
/** Predicted pearl flight arc from throw direction, not an authoritative landing result. */
public final class PearlTrajectory extends Module {
 private final NumberSetting steps=add(new NumberSetting("Arc steps",50,10,90,5));
 private final NumberSetting thickness=add(new NumberSetting("Line width",.020,.008,.06,.002));
 private final BoolSetting endpoint=add(new BoolSetting("Endpoint ring",true));
 public PearlTrajectory(){super("PearlTrajectory",Category.VISUALS);describe("Estimated ender pearl trajectory with optional end ring (no collision prediction).");}
 @Override public void onRender3D(WorldRenderContext ctx,PoseStack stack){
   if(mc.player==null||mc.level==null||!mc.player.getMainHandItem().is(Items.ENDER_PEARL))return;
   var cam=ctx.camera().position();Matrix4f m=stack.last().pose();MeshBuilder b=Fx.colorQuadsDepth();
   double pitch=Math.toRadians(mc.player.getXRot()),yaw=Math.toRadians(mc.player.getYRot());
   double vx=-Math.sin(yaw)*Math.cos(pitch)*1.5,vy=-Math.sin(pitch)*1.5,vz=Math.cos(yaw)*Math.cos(pitch)*1.5;
   double x=mc.player.getX(),y=mc.player.getEyeY()-.12,z=mc.player.getZ();
   int n=VisualOptimizer.budget(steps.getI(),10);
   for(int i=0;i<n;i++){
      double nx=x+vx,ny=y+vy,nz=z+vz;int c=Colors.alpha(Colors.wave(i/(float)n),180);
      Vfx.tube(b,m,x,y,z,nx,ny,nz,thickness.getF(),c,c,cam.x,cam.y,cam.z);
      x=nx;y=ny;z=nz;vx*=.99;vz*=.99;vy=vy*.99-.03;
      if (y < mc.level.getMinY() || y > mc.level.getMinY() + mc.level.getHeight()) break;
   }
   if(endpoint.get())Vfx.ringFlat(b,m,x,y,z,.30,.33,20,Colors.alpha(Colors.C3,130),Colors.alpha(Colors.C3,0));
   Fx.draw();
 }
}
