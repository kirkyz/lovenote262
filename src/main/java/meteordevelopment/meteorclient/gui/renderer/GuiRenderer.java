package meteordevelopment.meteorclient.gui.renderer;

import meteordevelopment.meteorclient.gui.renderer.packer.TextureRegion;

public class GuiRenderer {
    public void text(String text, double x, double y) {}
    public void texture(TextureRegion region, double x, double y, double width, double height) {}
    public void quad(double x, double y, double width, double height) {}
    public void roundedQuad(double x, double y, double width, double height, double radius) {}
}
