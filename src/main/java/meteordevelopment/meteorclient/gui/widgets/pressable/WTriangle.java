/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/lovenote262).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.pressable;

public abstract class WTriangle extends WPressable {
    public double rotation;

    @Override
    protected void onCalculateSize() {
        double s = theme.textHeight();

        width = s;
        height = s;
    }
}
