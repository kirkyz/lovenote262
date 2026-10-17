package meteordevelopment.meteorclient.gui;

public class GuiThemes {
    private static GuiTheme theme;

    public static GuiTheme get() {
        if (theme == null) theme = new GuiTheme("Meteor");
        return theme;
    }

    public static void init() { get(); }
}
