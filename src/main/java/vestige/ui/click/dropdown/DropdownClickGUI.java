package vestige.ui.click.dropdown;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import vestige.Vestige;
import vestige.font.VestigeFontRenderer;
import vestige.module.Category;
import vestige.module.Module;
import vestige.module.impl.visual.ClickGuiModule;
import vestige.setting.impl.*;
import vestige.ui.click.dropdown.impl.CategoryHolder;
import vestige.ui.click.dropdown.impl.ModuleHolder;
import vestige.ui.click.dropdown.impl.SettingHolder;
import vestige.util.render.ColorUtil;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

import static org.lwjgl.opengl.GL11.*;

public class DropdownClickGUI extends GuiScreen {

    private final ClickGuiModule module;
    private final ArrayList<CategoryHolder> categories = new ArrayList<>();
    private final int categoryXOffset = 140;
    private final int categoryYOffset = 28;
    private final int moduleYOffset = 22;
    private final int settingYOffset = 18;

    private final Color backgroundColor = new Color(20, 20, 25, 160);
    private final Color categoryColor = new Color(25, 25, 30, 180);
    private final Color moduleDisabledColor = new Color(30, 30, 35, 160);
    private final Color textColor = new Color(230, 230, 230);

    private IntegerSetting enabledColorR;
    private IntegerSetting enabledColorG;
    private IntegerSetting enabledColorB;
    private IntegerSetting animSpeed;

    private final int mouseHoverColor = 0x20FFFFFF;

    private int lastMouseX, lastMouseY;
    private Module keyChangeModule;
    private int scrollY;

    private long openTime;

    public DropdownClickGUI(ClickGuiModule module) {
        this.module = module;

        enabledColorR = new IntegerSetting("Enabled Red", 60, 0, 255, 1);
        enabledColorG = new IntegerSetting("Enabled Green", 120, 0, 255, 1);
        enabledColorB = new IntegerSetting("Enabled Blue", 200, 0, 255, 1);
        animSpeed = new IntegerSetting("Animation Speed", 300, 100, 1000, 50);

        module.addSettings(enabledColorR, enabledColorG, enabledColorB, animSpeed);

        int x = 40;
        int y = 60;

        for (Category category : Category.values()) {
            ArrayList<ModuleHolder> modules = new ArrayList<>();
            Vestige.instance.getModuleManager().modules.stream()
                    .filter(m -> m.getCategory() == category)
                    .forEach(m -> modules.add(new ModuleHolder(m)));

            categories.add(new CategoryHolder(category, modules, x, y, true));
            x += categoryXOffset + 30;
        }
    }

    @Override
    public void initGui() {
        categories.forEach(c -> c.getModules().forEach(m -> m.updateState()));
        scrollY = 0;
        openTime = System.currentTimeMillis();
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        float a = (color >> 24 & 0xFF) / 255.0F;
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        glColor4f(r, g, b, a);
        glEnable(GL_LINE_SMOOTH);
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);

        glBegin(GL_TRIANGLE_FAN);

        glVertex2f(x + width / 2, y + height / 2);

        for(int i = 0; i <= 90; i += 3) {
            double angle = Math.toRadians(i + 180);
            glVertex2d(x + radius + Math.sin(angle) * radius, y + radius + Math.cos(angle) * radius);
        }

        for(int i = 90; i <= 180; i += 3) {
            double angle = Math.toRadians(i + 180);
            glVertex2d(x + radius + Math.sin(angle) * radius, y + height - radius + Math.cos(angle) * radius);
        }

        for(int i = 180; i <= 270; i += 3) {
            double angle = Math.toRadians(i + 180);
            glVertex2d(x + width - radius + Math.sin(angle) * radius, y + height - radius + Math.cos(angle) * radius);
        }

        for(int i = 270; i <= 360; i += 3) {
            double angle = Math.toRadians(i + 180);
            glVertex2d(x + width - radius + Math.sin(angle) * radius, y + radius + Math.cos(angle) * radius);
        }

        glVertex2d(x + radius, y);

        glEnd();

        glDisable(GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private float getSettingsAnimation(ModuleHolder holder) {
        double elapsed = holder.getSettingsShownTimer().getTimeElapsed();
        float progress = Math.min(1.0f, (float)(elapsed / 250.0));

        return holder.isSettingsShown() ? easeOutCubic(progress) : 1.0f - easeOutCubic(progress);
    }

    private float easeOutCubic(float t) {
        return 1 - (float) Math.pow(1 - t, 3);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        VestigeFontRenderer fr = Vestige.instance.getFontManager().getProductSans();

        long elapsed = System.currentTimeMillis() - openTime;
        float progress = Math.min(1.0f, (float) elapsed / animSpeed.getValue());
        float scale = easeOutBack(progress);

        ScaledResolution sr = new ScaledResolution(mc);
        int centerX = sr.getScaledWidth() / 2;
        int centerY = sr.getScaledHeight() / 2;

        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY, 0);
        GL11.glScalef(scale, scale, 1);
        GL11.glTranslatef(-centerX, -centerY, 0);

        Color moduleEnabledColor = new Color(enabledColorR.getValue(), enabledColorG.getValue(), enabledColorB.getValue(), 160);
        Color accentColor = new Color(enabledColorR.getValue(), enabledColorG.getValue(), enabledColorB.getValue());

        for (CategoryHolder category : categories) {
            if (category.isShown()) {
                if(category.isHolded()) {
                    category.setX(category.getX() + mouseX - lastMouseX);
                    category.setY(category.getY() + mouseY - lastMouseY);
                }

                int x = category.getX();
                int y = category.getY() + scrollY;

                int totalHeight = categoryYOffset;
                for (ModuleHolder holder : category.getModules()) {
                    totalHeight += moduleYOffset;

                    float animProgress = getSettingsAnimation(holder);

                    if(animProgress > 0) {
                        int settingsHeight = settingYOffset;
                        for(SettingHolder sh : holder.getSettings()) {
                            if(sh.getSetting().getVisibility().get()) {
                                settingsHeight += settingYOffset;
                                if(sh.getSetting() instanceof ModeSetting) {
                                    ModeSetting ms = sh.getSetting();
                                    String toRender = ms.getName() + " : " + ms.getMode();
                                    if(fr.getStringWidth(toRender) > categoryXOffset - 10) {
                                        settingsHeight += 6;
                                    }
                                }
                            }
                        }
                        totalHeight += (int)(settingsHeight * animProgress);
                    }
                }

                drawRoundedRect(x, y, categoryXOffset, totalHeight, 8, backgroundColor.getRGB());

                drawRoundedRect(x, y, categoryXOffset, categoryYOffset, 8, categoryColor.getRGB());

                String categoryName = category.getCategory().toString().toLowerCase();
                String capital = categoryName.substring(0, 1).toUpperCase();
                String rest = categoryName.substring(1);

                fr.drawStringWithShadow(capital + rest, x + 10, y + 9, textColor.getRGB());

                float startX = x;
                float endX = startX + categoryXOffset;

                y += categoryYOffset;

                for (ModuleHolder holder : category.getModules()) {
                    Module m = holder.getModule();

                    float startY = y;
                    float endY = startY + moduleYOffset;

                    double mult = Math.min(1.0, holder.getTimer().getTimeElapsed() / 200.0);

                    Color baseColor = m.isEnabled() ? moduleEnabledColor : moduleDisabledColor;
                    Color targetColor = m.isEnabled() ? moduleEnabledColor : moduleDisabledColor;
                    Color fromColor = m.isEnabled() ? moduleDisabledColor : moduleEnabledColor;

                    if(mult < 1.0) {
                        Color interpolated = ColorUtil.getGradient(fromColor, targetColor, mult);
                        baseColor = new Color(interpolated.getRed(), interpolated.getGreen(), interpolated.getBlue(), 160);
                    } else {
                        baseColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 160);
                    }

                    drawRect((int)(startX + 3), (int)startY, (int)(endX - 3), (int)endY, baseColor.getRGB());

                    if(mouseX > startX && mouseX < endX && mouseY > startY && mouseY < endY) {
                        drawRect((int)(startX + 3), (int)startY, (int)(endX - 3), (int)endY, mouseHoverColor);
                    }

                    fr.drawStringWithShadow(m.getName(), startX + 10, startY + 7, textColor.getRGB());

                    if(holder.getSettings().size() > 0) {
                        float rotationAngle = getSettingsAnimation(holder) * 180;
                        GL11.glPushMatrix();
                        GL11.glTranslatef(endX - 11.5f, startY + 11, 0);
                        GL11.glRotatef(rotationAngle, 0, 0, 1);
                        GL11.glTranslatef(-(endX - 11.5f), -(startY + 11), 0);
                        fr.drawStringWithShadow("+", endX - 15, startY + 6, textColor.getRGB());
                        GL11.glPopMatrix();
                    }

                    y += moduleYOffset;

                    float animProgress = getSettingsAnimation(holder);

                    if(animProgress > 0) {
                        GL11.glPushMatrix();

                        GL11.glEnable(GL11.GL_SCISSOR_TEST);
                        ScaledResolution scissorSr = new ScaledResolution(mc);
                        int scissorScale = scissorSr.getScaleFactor();

                        float settingsStartY = y;
                        int maxSettingsHeight = settingYOffset;
                        for(SettingHolder sh : holder.getSettings()) {
                            if(sh.getSetting().getVisibility().get()) {
                                maxSettingsHeight += settingYOffset;
                                if(sh.getSetting() instanceof ModeSetting) {
                                    ModeSetting ms = sh.getSetting();
                                    String toRender = ms.getName() + " : " + ms.getMode();
                                    if(fr.getStringWidth(toRender) > categoryXOffset - 10) {
                                        maxSettingsHeight += 6;
                                    }
                                }
                            }
                        }

                        int actualHeight = (int)(maxSettingsHeight * animProgress);

                        GL11.glScissor(
                                (int)(startX * scissorScale),
                                (int)((scissorSr.getScaledHeight() - (settingsStartY + actualHeight)) * scissorScale),
                                (int)(categoryXOffset * scissorScale),
                                (int)(actualHeight * scissorScale)
                        );

                        float startKeybindY = y;
                        float endKeybindY = y + settingYOffset;

                        drawRoundedRect(startX + 6, startKeybindY, categoryXOffset - 12, settingYOffset, 4,
                                new Color(35, 35, 40, 160).getRGB());

                        if(mouseX > startX + 6 && mouseX < endX - 6 && mouseY > startKeybindY && mouseY < endKeybindY) {
                            drawRoundedRect(startX + 6, startKeybindY, categoryXOffset - 12, settingYOffset, 4, mouseHoverColor);
                        }

                        fr.drawStringWithShadow(keyChangeModule == m ? "Waiting..." : "Keybind: " + Keyboard.getKeyName(m.getKey()),
                                startX + 12, startKeybindY + 5, new Color(190, 190, 190).getRGB());

                        y += settingYOffset;

                        for(SettingHolder settingHolder : holder.getSettings()) {
                            if(settingHolder.getSetting().getVisibility().get()) {
                                float startSettingY = y;
                                float endSettingY = y + settingYOffset;

                                drawRoundedRect(startX + 6, startSettingY, categoryXOffset - 12, settingYOffset, 4,
                                        new Color(35, 35, 40, 160).getRGB());

                                boolean hoveringSetting = mouseX > startX + 6 && mouseX < endX - 6 &&
                                        mouseY > startSettingY && mouseY < endSettingY;

                                if(settingHolder.getSetting() instanceof ModeSetting) {
                                    ModeSetting setting = settingHolder.getSetting();
                                    String toRender = setting.getName() + " : " + setting.getMode();

                                    if(fr.getStringWidth(toRender) > categoryXOffset - 10) {
                                        drawRoundedRect(startX + 6, endSettingY, categoryXOffset - 12, 6, 4,
                                                new Color(35, 35, 40, 160).getRGB());

                                        if(mouseX > startX + 6 && mouseX < endX - 6 && mouseY > startSettingY && mouseY < endSettingY + 6) {
                                            drawRoundedRect(startX + 6, startSettingY, categoryXOffset - 12, settingYOffset + 6, 4, mouseHoverColor);
                                        }

                                        fr.drawStringWithShadow(setting.getName() + " :", startX + 12, startSettingY + 2, new Color(190, 190, 190).getRGB());
                                        fr.drawStringWithShadow(setting.getMode(), startX + 12, startSettingY + 11, accentColor.getRGB());
                                        y += 6;
                                    } else {
                                        if(hoveringSetting) {
                                            drawRoundedRect(startX + 6, startSettingY, categoryXOffset - 12, settingYOffset, 4, mouseHoverColor);
                                        }
                                        fr.drawStringWithShadow(toRender, startX + 12, startSettingY + 5, new Color(190, 190, 190).getRGB());
                                    }
                                } else {
                                    if(hoveringSetting) {
                                        drawRoundedRect(startX + 6, startSettingY, categoryXOffset - 12, settingYOffset, 4, mouseHoverColor);
                                    }

                                    if(settingHolder.getSetting() instanceof BooleanSetting) {
                                        BooleanSetting setting = settingHolder.getSetting();

                                        fr.drawStringWithShadow(setting.getName(), startX + 12, startSettingY + 5, new Color(190, 190, 190).getRGB());

                                        float toggleX = endX - 30;
                                        float toggleY = startSettingY + 4;
                                        float toggleWidth = 22;
                                        float toggleHeight = 11;

                                        drawRoundedRect(toggleX, toggleY, toggleWidth, toggleHeight, 5.5f,
                                                setting.isEnabled() ? accentColor.getRGB() : new Color(50, 50, 55, 160).getRGB());

                                        float circleX = setting.isEnabled() ? toggleX + toggleWidth - 9 : toggleX + 2;
                                        drawRoundedRect(circleX, toggleY + 1.5f, 7, 8, 4, -1);

                                    } else if(settingHolder.getSetting() instanceof EnumModeSetting) {
                                        EnumModeSetting setting = settingHolder.getSetting();
                                        fr.drawStringWithShadow(setting.getName() + " : " + setting.getMode().name(),
                                                startX + 12, startSettingY + 5, new Color(190, 190, 190).getRGB());

                                    } else if(settingHolder.getSetting() instanceof DoubleSetting) {
                                        DoubleSetting setting = settingHolder.getSetting();

                                        float startSettingX = startX + 12;
                                        float endSettingX = endX - 12;
                                        float length = endSettingX - startSettingX;

                                        if (settingHolder.isHoldingMouse() && mouseX >= startSettingX && mouseX <= endSettingX &&
                                                mouseY > startSettingY && mouseY < endSettingY) {
                                            double mousePos = mouseX - startSettingX;
                                            double thing = (mousePos / length);
                                            setting.setValue(thing * (setting.getMax() - setting.getMin()) + setting.getMin());
                                        }

                                        double numberX = startSettingX + ((setting.getValue() - setting.getMin()) * length / (setting.getMax() - setting.getMin()));

                                        drawRoundedRect(startSettingX, startSettingY + 11, length, 3, 1.5f,
                                                new Color(45, 45, 50, 160).getRGB());
                                        drawRoundedRect(startSettingX, startSettingY + 11, (float)(numberX - startSettingX), 3, 1.5f,
                                                accentColor.getRGB());

                                        fr.drawStringWithShadow(setting.getName() + " : " + setting.getStringValue(),
                                                startSettingX, startSettingY + 1, new Color(190, 190, 190).getRGB());

                                    } else if(settingHolder.getSetting() instanceof IntegerSetting) {
                                        IntegerSetting setting = settingHolder.getSetting();

                                        float startSettingX = startX + 12;
                                        float endSettingX = endX - 12;
                                        float length = endSettingX - startSettingX;

                                        if (settingHolder.isHoldingMouse() && mouseX >= startSettingX && mouseX <= endSettingX &&
                                                mouseY > startSettingY && mouseY < endSettingY) {
                                            double mousePos = mouseX - startSettingX;
                                            double thing = (mousePos / length);
                                            int value = (int) (thing * (setting.getMax() - setting.getMin()) + setting.getMin());
                                            setting.setValue(value);
                                        }

                                        double numberX = startSettingX + ((setting.getValue() - setting.getMin()) * length / (setting.getMax() - setting.getMin()));

                                        drawRoundedRect(startSettingX, startSettingY + 11, length, 3, 1.5f,
                                                new Color(45, 45, 50, 160).getRGB());
                                        drawRoundedRect(startSettingX, startSettingY + 11, (float)(numberX - startSettingX), 3, 1.5f,
                                                accentColor.getRGB());

                                        fr.drawStringWithShadow(setting.getName() + " : " + setting.getValue(),
                                                startSettingX, startSettingY + 1, new Color(190, 190, 190).getRGB());
                                    }
                                }

                                y += settingYOffset;
                            }
                        }

                        GL11.glDisable(GL11.GL_SCISSOR_TEST);
                        GL11.glPopMatrix();

                        y = (int)settingsStartY + actualHeight;
                    }
                }
            }
        }

        GL11.glPopMatrix();

        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    private float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        VestigeFontRenderer fr = Vestige.instance.getFontManager().getProductSans();

        for (CategoryHolder category : categories) {
            if(category.isShown()) {
                int x = category.getX();
                int y = category.getY() + scrollY;

                if (mouseX > x && mouseX < x + categoryXOffset && mouseY > y && mouseY < y + categoryYOffset) {
                    category.setHolded(true);
                }

                float startX = x;
                float endX = startX + categoryXOffset;

                y += categoryYOffset;

                for (ModuleHolder holder : category.getModules()) {
                    Module m = holder.getModule();

                    float startY = y;
                    float endY = startY + moduleYOffset;

                    if(mouseX > startX && mouseX < endX && mouseY > startY && mouseY < endY) {
                        if(button == 0) {
                            m.toggle();
                            holder.updateState();
                        } else if(button == 1) {
                            holder.setSettingsShown(!holder.isSettingsShown());
                        }
                    }

                    y += moduleYOffset;

                    float animProgress = getSettingsAnimation(holder);

                    if(animProgress > 0) {
                        int settingsHeight = settingYOffset;
                        for(SettingHolder sh : holder.getSettings()) {
                            if(sh.getSetting().getVisibility().get()) {
                                settingsHeight += settingYOffset;
                                if(sh.getSetting() instanceof ModeSetting) {
                                    ModeSetting ms = sh.getSetting();
                                    String toRender = ms.getName() + " : " + ms.getMode();
                                    if(fr.getStringWidth(toRender) > categoryXOffset - 10) {
                                        settingsHeight += 6;
                                    }
                                }
                            }
                        }

                        if(animProgress >= 0.99) {
                            float startKeybindY = y;
                            float endKeybindY = y + settingYOffset;

                            if(button == 0 && mouseX > startX + 6 && mouseX < endX - 6 && mouseY > startKeybindY && mouseY < endKeybindY) {
                                keyChangeModule = m;
                            }

                            y += settingYOffset;

                            for(SettingHolder settingHolder : holder.getSettings()) {
                                if(settingHolder.getSetting().getVisibility().get()) {
                                    float startSettingY = y;
                                    float endSettingY = y + settingYOffset;

                                    boolean hovering = mouseX > startX + 6 && mouseX < endX - 6 && mouseY > startSettingY && mouseY < endSettingY;

                                    if(settingHolder.getSetting() instanceof BooleanSetting) {
                                        BooleanSetting setting = settingHolder.getSetting();
                                        if(button == 0 && hovering) {
                                            setting.setEnabled(!setting.isEnabled());
                                        }
                                    } else if(settingHolder.getSetting() instanceof ModeSetting) {
                                        ModeSetting setting = settingHolder.getSetting();
                                        String toRender = setting.getName() + " : " + setting.getMode();

                                        if(fr.getStringWidth(toRender) > categoryXOffset - 10) {
                                            if(mouseX > startX + 6 && mouseX < endX - 6 && mouseY > startSettingY && mouseY < endSettingY + 6) {
                                                if(button == 0) {
                                                    setting.increment();
                                                } else if(button == 1) {
                                                    setting.decrement();
                                                }
                                            }
                                            y += 6;
                                        } else {
                                            if(hovering) {
                                                if(button == 0) {
                                                    setting.increment();
                                                } else if(button == 1) {
                                                    setting.decrement();
                                                }
                                            }
                                        }
                                    } else if(settingHolder.getSetting() instanceof EnumModeSetting) {
                                        EnumModeSetting setting = settingHolder.getSetting();
                                        if(hovering) {
                                            if(button == 0) {
                                                setting.increment();
                                            } else if(button == 1) {
                                                setting.decrement();
                                            }
                                        }
                                    }

                                    if(hovering && button == 0) {
                                        settingHolder.setHoldingMouse(true);
                                    }

                                    y += settingYOffset;
                                }
                            }
                        } else {
                            y += (int)(settingsHeight * animProgress);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        categories.forEach(c -> {
            c.setHolded(false);
            c.getModules().forEach(m -> m.getSettings().forEach(s -> s.setHoldingMouse(false)));
        });
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int i = Integer.signum(Mouse.getEventDWheel());
        scrollY += i * 15;
        scrollY = Math.max(-500, Math.min(500, scrollY));
    }

    @Override
    protected void keyTyped(char typedChar, int key) throws IOException {
        if (key == 1) {
            this.mc.displayGuiScreen(null);
            if (this.mc.currentScreen == null) {
                this.mc.setIngameFocus();
            }
        }

        if(keyChangeModule != null) {
            keyChangeModule.setKey(key == 14 ? 0 : key);
            keyChangeModule = null;
        }
    }

    @Override
    public void onGuiClosed() {
        module.setEnabled(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
