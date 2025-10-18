package vestige.ui.menu;
import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import java.io.IOException;
import org.lwjgl.opengl.GL11;
import net.minecraft.util.ResourceLocation;
import vestige.Vestige;
import vestige.font.VestigeFontRenderer;
import vestige.ui.menu.AltLoginScreen;

public class VestigeMainMenu extends GuiScreen {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final String[] buttons = {"Single player", "Multi player", "Alt Manager", "Options", "Shutdown"};
    private final String[] icons = {"single_player", "multi_player", "alt_manager", "options", "shutdown"};
    private long initTime = System.currentTimeMillis();
    private VestigeFontRenderer productSans;
    private ResourceLocation backgroundTexture = new ResourceLocation("minecraft", "lycanis/image/background.png");

    @Override
    public void initGui() {
        this.buttonList.clear();
        productSans = Vestige.instance.getFontManager().getProductSans();
        ScaledResolution sr = new ScaledResolution(mc);
        int buttonWidth = 200;
        int buttonHeight = 32;
        int centerX = sr.getScaledWidth() / 2;
        int startY = sr.getScaledHeight() / 2 - 80;

        for (int i = 0; i < buttons.length; i++) {
            int x = centerX - buttonWidth / 2;
            int y = startY + i * (buttonHeight + 4);
            this.buttonList.add(new CleanButton(i, x, y, buttonWidth, buttonHeight, buttons[i], icons[i]));
        }
    }

    private void drawBlurredBackground(int width, int height) {
        mc.getTextureManager().bindTexture(backgroundTexture);
        GlStateManager.enableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        drawModalRectWithCustomSizedTexture(0, 0, 0, 0, width, height, width, height);
        GlStateManager.disableBlend();
    }

    private void drawMenuPanel(int width, int height) {
        float panelWidth = 220;
        float panelHeight = 200;
        float panelX = width / 2f - panelWidth / 2f;
        float panelY = height / 2f - panelHeight / 2f;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        drawRoundedRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 8,
                new Color(25, 25, 30, 220));

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private void drawRoundedRect(float left, float top, float right, float bottom, float radius, Color color) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = color.getAlpha() / 255.0f;

        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        wr.pos(left + radius, top, 0).color(r, g, b, a).endVertex();
        wr.pos(right - radius, top, 0).color(r, g, b, a).endVertex();
        wr.pos(right - radius, bottom, 0).color(r, g, b, a).endVertex();
        wr.pos(left + radius, bottom, 0).color(r, g, b, a).endVertex();

        wr.pos(left, top + radius, 0).color(r, g, b, a).endVertex();
        wr.pos(left + radius, top + radius, 0).color(r, g, b, a).endVertex();
        wr.pos(left + radius, bottom - radius, 0).color(r, g, b, a).endVertex();
        wr.pos(left, bottom - radius, 0).color(r, g, b, a).endVertex();

        wr.pos(right - radius, top + radius, 0).color(r, g, b, a).endVertex();
        wr.pos(right, top + radius, 0).color(r, g, b, a).endVertex();
        wr.pos(right, bottom - radius, 0).color(r, g, b, a).endVertex();
        wr.pos(right - radius, bottom - radius, 0).color(r, g, b, a).endVertex();

        tess.draw();

        for (int i = 0; i < 16; i++) {
            float angle1 = (float) (i * Math.PI / 32);
            float angle2 = (float) ((i + 1) * Math.PI / 32);

            wr.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);

            wr.pos(left + radius, top + radius, 0).color(r, g, b, a).endVertex();
            wr.pos(left + radius + Math.cos(angle1 + Math.PI) * radius,
                    top + radius + Math.sin(angle1 + Math.PI) * radius, 0).color(r, g, b, a).endVertex();
            wr.pos(left + radius + Math.cos(angle2 + Math.PI) * radius,
                    top + radius + Math.sin(angle2 + Math.PI) * radius, 0).color(r, g, b, a).endVertex();

            wr.pos(right - radius, top + radius, 0).color(r, g, b, a).endVertex();
            wr.pos(right - radius + Math.cos(angle1 - Math.PI / 2) * radius,
                    top + radius + Math.sin(angle1 - Math.PI / 2) * radius, 0).color(r, g, b, a).endVertex();
            wr.pos(right - radius + Math.cos(angle2 - Math.PI / 2) * radius,
                    top + radius + Math.sin(angle2 - Math.PI / 2) * radius, 0).color(r, g, b, a).endVertex();

            wr.pos(right - radius, bottom - radius, 0).color(r, g, b, a).endVertex();
            wr.pos(right - radius + Math.cos(angle1) * radius,
                    bottom - radius + Math.sin(angle1) * radius, 0).color(r, g, b, a).endVertex();
            wr.pos(right - radius + Math.cos(angle2) * radius,
                    bottom - radius + Math.sin(angle2) * radius, 0).color(r, g, b, a).endVertex();

            wr.pos(left + radius, bottom - radius, 0).color(r, g, b, a).endVertex();
            wr.pos(left + radius + Math.cos(angle1 + Math.PI / 2) * radius,
                    bottom - radius + Math.sin(angle1 + Math.PI / 2) * radius, 0).color(r, g, b, a).endVertex();
            wr.pos(left + radius + Math.cos(angle2 + Math.PI / 2) * radius,
                    bottom - radius + Math.sin(angle2 + Math.PI / 2) * radius, 0).color(r, g, b, a).endVertex();

            tess.draw();
        }

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution sr = new ScaledResolution(mc);

        drawBlurredBackground(sr.getScaledWidth(), sr.getScaledHeight());
        drawMenuPanel(sr.getScaledWidth(), sr.getScaledHeight());

        String title = "Tellurium";
        int titleWidth = this.fontRendererObj.getStringWidth(title) * 3;
        int titleX = sr.getScaledWidth() / 2 - titleWidth / 2;
        int titleY = sr.getScaledHeight() / 2 - 140;

        GlStateManager.pushMatrix();
        GlStateManager.scale(3.0f, 3.0f, 1.0f);
        this.fontRendererObj.drawString(title, titleX / 3, titleY / 3, 0xFFFFFF);
        GlStateManager.popMatrix();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0:
                mc.displayGuiScreen(new GuiSelectWorld(this));
                break;
            case 1:
                mc.displayGuiScreen(new GuiMultiplayer(this));
                break;
            case 2:
                mc.displayGuiScreen(new AltLoginScreen());
                break;
            case 3:
                mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
                break;
            case 4:
                mc.shutdown();
                break;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private class CleanButton extends GuiButton {
        private float hoverAnimation = 0.0f;
        private String iconName;
        private ResourceLocation iconTexture;

        public CleanButton(int id, int x, int y, int width, int height, String text, String iconName) {
            super(id, x, y, width, height, text);
            this.iconName = iconName;
            this.iconTexture = new ResourceLocation("minecraft", "lycanis/image/" + iconName + ".png");
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (this.visible) {
                boolean hovered = mouseX >= this.xPosition && mouseY >= this.yPosition &&
                        mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

                if (hovered) {
                    hoverAnimation = Math.min(1.0f, hoverAnimation + 0.08f);
                } else {
                    hoverAnimation = Math.max(0.0f, hoverAnimation - 0.08f);
                }

                float scale = 1.05f - 0.05f * (1.0f - hoverAnimation);
                GlStateManager.pushMatrix();
                GlStateManager.translate(this.xPosition + this.width / 2f, this.yPosition + this.height / 2f, 0);
                GlStateManager.scale(scale, scale, 1.0f);
                GlStateManager.translate(-(this.xPosition + this.width / 2f), -(this.yPosition + this.height / 2f), 0);

                Color bgColor = new Color(30, 30, 35, 180);
                drawRoundedRect(this.xPosition, this.yPosition,
                        this.xPosition + this.width, this.yPosition + this.height,
                        6, bgColor);

                int textColor = hovered ? 0xFFFFFF : 0xCCCCCC;
                int iconX = this.xPosition + 12;
                int textX = this.xPosition + 35;
                int textY = this.yPosition + (this.height - 8) / 2;

                mc.getTextureManager().bindTexture(this.iconTexture);
                GlStateManager.enableBlend();
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                drawModalRectWithCustomSizedTexture(iconX, textY - 4, 0, 0, 12, 12, 12, 12);
                GlStateManager.disableBlend();

                productSans.drawStringWithShadow(this.displayString, textX, textY, textColor);
                GlStateManager.popMatrix();
            }
        }
    }
}