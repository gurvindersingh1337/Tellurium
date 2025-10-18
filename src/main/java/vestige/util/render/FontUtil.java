package vestige.util.render;

import net.minecraft.client.gui.FontRenderer;
import vestige.Vestige;
import vestige.font.VestigeFontRenderer;
import vestige.setting.impl.ModeSetting;
import vestige.util.IMinecraft;

import java.util.function.Supplier;

public class FontUtil implements IMinecraft {

    private static FontRenderer mcFont;
    private static VestigeFontRenderer productSans, comfortaa;

    public static ModeSetting getFontSetting() {
        return new ModeSetting("Font", "Minecraft", "Minecraft", "Product sans", "Comfortaa");
    }

    public static ModeSetting getFontSetting(Supplier<Boolean> visibility) {
        return new ModeSetting("Font", visibility, "Minecraft", "Minecraft", "Product sans", "Comfortaa");
    }

    public static void initFonts() {
        mcFont = mc.fontRendererObj;
        if (Vestige.instance.getFontManager() != null) {
            productSans = Vestige.instance.getFontManager().getProductSans();
            comfortaa = Vestige.instance.getFontManager().getComfortaa();
        }
    }

    public static void drawString(String font, String text, float x, float y, int color) {
        if (text == null) return;
        try {
            switch (font) {
                case "Product sans":
                    if (productSans != null) {
                        productSans.drawString(text, x, y, color);
                        return;
                    }
                    break;
                case "Comfortaa":
                    if (comfortaa != null) {
                        comfortaa.drawString(text, x, y, color);
                        return;
                    }
                    break;
                default:
                    if (mcFont != null) {
                        mcFont.drawString(text, x, y, color);
                    }
                    break;
            }
        } catch (Exception ignored) {}
    }

    public static void drawStringWithShadow(String font, String text, float x, float y, int color) {
        if (text == null) return;
        try {
            switch (font) {
                case "Product sans":
                    if (productSans != null) {
                        productSans.drawStringWithShadow(text, x, y, color);
                        return;
                    }
                    break;
                case "Comfortaa":
                    if (comfortaa != null) {
                        comfortaa.drawStringWithShadow(text, x, y, color);
                        return;
                    }
                    break;
                default:
                    if (mcFont != null) {
                        mcFont.drawStringWithShadow(text, x, y, color);
                    }
                    break;
            }
        } catch (Exception ignored) {}
    }

    public static double getStringWidth(String font, String s) {
        if (s == null) return 0;
        try {
            switch (font) {
                case "Product sans":
                    return productSans != null ? productSans.getStringWidth(s) : mc.fontRendererObj.getStringWidth(s);
                case "Comfortaa":
                    return comfortaa != null ? comfortaa.getStringWidth(s) : mc.fontRendererObj.getStringWidth(s);
                default:
                    return mc.fontRendererObj.getStringWidth(s);
            }
        } catch (Exception e) {
            return mc.fontRendererObj.getStringWidth(s);
        }
    }

    public static int getFontHeight(String font) {
        try {
            switch (font) {
                case "Product sans":
                    return productSans != null ? productSans.getHeight() : mc.fontRendererObj.FONT_HEIGHT;
                case "Comfortaa":
                    return comfortaa != null ? comfortaa.getHeight() : mc.fontRendererObj.FONT_HEIGHT;
                default:
                    return mc.fontRendererObj.FONT_HEIGHT;
            }
        } catch (Exception e) {
            return mc.fontRendererObj.FONT_HEIGHT;
        }
    }
}
