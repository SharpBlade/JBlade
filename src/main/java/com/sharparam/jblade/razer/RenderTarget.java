package com.sharparam.jblade.razer;

import com.sharparam.jblade.annotations.APIComponent;
import com.sharparam.jblade.integration.Renderer;
import com.sharparam.jblade.razer.exceptions.RazerNativeException;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;

import javax.swing.*;
import java.awt.image.BufferedImage;

/**
 * Created on 2014-07-19.
 *
 * @author Sharparam
 */
public abstract class RenderTarget {
    private final int displayHeight;
    private final int displayWidth;
    private final RazerAPI.TargetDisplay targetDisplay;
    private final RazerAPI razerAPI;

    protected RenderTarget(final RazerAPI.TargetDisplay target, final int height, final int width) {
        targetDisplay = target;
        displayHeight = height;
        displayWidth = width;
        razerAPI = RazerAPI.INSTANCE;
    }

    @APIComponent
    public abstract String getCurrentImage();

    @APIComponent
    public int getDisplayHeight() {
        return displayHeight;
    }

    @APIComponent
    public int getDisplayWidth() {
        return displayWidth;
    }

    @APIComponent
    public RazerAPI.TargetDisplay getTargetDisplay() {
        return targetDisplay;
    }

    @APIComponent
    public void drawFrame(final JFrame frame) throws RazerNativeException {
        final BufferedImage image = Renderer.renderComponent(frame);
        drawImage(image);
    }

    // TODO: Test this
    @APIComponent
    public void drawImage(final BufferedImage image) throws RazerNativeException {
        if (image.getType() != BufferedImage.TYPE_USHORT_565_RGB)
            throw new IllegalArgumentException("BufferedImage needs to be of type RGB565");

        final RazerAPI.BufferParams.ByValue params = new RazerAPI.BufferParams.ByValue();
        params.pixelType = RazerAPI.PixelType.RGB565;

        final int size = image.getWidth() * image.getHeight() * 2; // 2 == size of ushort
        params.dataSize = new WinDef.UINT(size);

        final int[] data = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, 0);

        //final WinDef.USHORT[] apiData = new WinDef.USHORT[data.length];
        //for (int i = 0; i < data.length; i++)
        //    apiData[i] = new WinDef.USHORT(data[i]);

        final Pointer dataPtr = new Memory(data.length);
        dataPtr.write(0, data, 0, data.length);

        params.ptrData = dataPtr;

        final RazerAPI.Hresult result = razerAPI.RzSBRenderBuffer(RazerAPI.TargetDisplay.WIDGET, params);

        if (result.isError())
            throw new RazerNativeException("RzSBRenderBuffer", result);
    }

    @APIComponent
    public abstract void setImage(final String image) throws RazerNativeException;
}
