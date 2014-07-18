package com.sharparam.jblade.integration;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Created by on 2014-02-07.
 *
 * @author Sharparam
 */
public class Renderer {
    public static BufferedImage renderComponent(Component component) {
        BufferedImage result = new BufferedImage(component.getWidth(),
                component.getHeight(), BufferedImage.TYPE_USHORT_565_RGB);
        component.paint(result.getGraphics());
        return result;
    }
}
