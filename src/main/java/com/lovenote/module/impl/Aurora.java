package com.lovenote.module.impl;

import com.lovenote.module.Category;
import com.lovenote.module.Module;
import com.lovenote.setting.ModeSetting;
import com.lovenote.setting.NumberSetting;
import com.lovenote.util.Colors;
import com.lovenote.util.Fx;
import com.lovenote.util.Tex;
import com.lovenote.util.Vfx;
import com.lovenote.util.WorldRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import org.joml.Matrix4f;

/** Native 26.2 visual effect; no server packets or world mutation. */
public final class Aurora extends Module {
    private final ModeSetting palette = add(new ModeSetting("Colour", "Theme", "Theme", "Rainbow", "Ice", "Pink", "White"));
    private int tint(float f) {
        return switch (palette.get()) {
            case "Rainbow" -> Colors.rainbow(f);
            case "Ice" -> 0xFF93E6FF;
            case "Pink" -> 0xFFFF7AD9;
            case "White" -> 0xFFFFFFFF;
            default -> Colors.wave(f);
        };
    }
    private final NumberSetting curtains = add(new NumberSetting("Curtains", 3, 1, 9, 1));
    private final NumberSetting distance = add(new NumberSetting("Distance", 44, 8, 90, 1));
    private final NumberSetting height = add(new NumberSetting("Height", 12, 4, 55, 1));
    private final NumberSetting width = add(new NumberSetting("Width", 12, 4, 40, 1));
    private final NumberSetting speed = add(new NumberSetting("Wave speed", 0.5, 0, 3, 0.05));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 55, 5, 160, 5));

    public Aurora() { super("Aurora", Category.VISUALS); describe("Animated coloured aurora curtains in the distance, with height and speed controls."); }
    @Override public void onRender3D(WorldRenderContext ctx, PoseStack stack) {
        if(mc.player==null) return;
        Matrix4f mat=stack.last().pose();
        MeshBuilder b=Fx.colorQuadsDepth();
        double t=System.currentTimeMillis()*.001*speed.get();
        // Thin waving bands high above the horizon. A single huge vertical
        // quadrilateral looked like a translucent rectangular sky texture.
        int n=curtains.getI();
        for(int k=0;k<n;k++) {
            double bearing=k*Math.PI*2/n;
            double ca=Math.cos(bearing),sa=Math.sin(bearing);
            for(int i=0;i<48;i++) {
                double u0=(i/48.0-.5),u1=((i+1)/48.0-.5);
                double edge0=Math.pow(Math.max(0,1-Math.abs(u0)*2),1.45);
                double edge1=Math.pow(Math.max(0,1-Math.abs(u1)*2),1.45);
                double s0=u0*width.get(),s1=u1*width.get();
                double wave0=Math.sin(t*0.7+s0*0.5+k)*1.8;
                double wave1=Math.sin(t*0.7+s1*0.5+k)*1.8;
                double d=distance.get()+k*2.5;
                double x0=mc.player.getX()+ca*d-sa*s0+ca*wave0;
                double z0=mc.player.getZ()+sa*d+ca*s0+sa*wave0;
                double x1=mc.player.getX()+ca*d-sa*s1+ca*wave1;
                double z1=mc.player.getZ()+sa*d+ca*s1+sa*wave1;
                double y=mc.player.getY()+17+Math.sin(t*.45+k)*2.5;
                // Three separate semitransparent wisps per curtain, not a solid wall.
                for(int band=0;band<3;band++) {
                    double low=band*2.2;
                    double bHeight=Math.min(height.get()/3.0,5.0);
                    int a0=(int)(Math.min(95,opacity.getI())*edge0*(.45+.15*band));
                    int a1=(int)(Math.min(95,opacity.getI())*edge1*(.45+.15*band));
                    int c0=Colors.alpha(tint((float)(k+.18*band)/n),a0);
                    int c1=Colors.alpha(tint((float)(k+.18*band)/n),a1);
                    int top0=Colors.alpha(c0,0),top1=Colors.alpha(c1,0);
                    Fx.vc(b,mat,(float)x0,(float)(y+low),(float)z0,c0);
                    Fx.vc(b,mat,(float)x1,(float)(y+low),(float)z1,c1);
                    Fx.vc(b,mat,(float)x1,(float)(y+low+bHeight+Math.sin(t+s1)*.9),(float)z1,top1);
                    Fx.vc(b,mat,(float)x0,(float)(y+low+bHeight+Math.sin(t+s0)*.9),(float)z0,top0);
                }
            }
        }
        Fx.draw();
    }

}
