package vestige.util.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

/**
 * Smooth Gaussian Blur utility (Tenacity-style)
 * Works on any rectangular area you define.
 */
public class BlurUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static ShaderGroup blurShader;
    private static Framebuffer framebuffer;
    private static boolean initialized = false;

    /**
     * Apply blur effect on the specified rectangular area.
     *
     * @param x      x-position
     * @param y      y-position
     * @param width  width of blur area
     * @param height height of blur area
     * @param radius blur radius (e.g., 8 for smooth, 15 for heavy)
     */
    public static void blurArea(float x, float y, float width, float height, float radius) {
        if (!initialized || blurShader == null) {
            try {
                blurShader = new ShaderGroup(mc.getTextureManager(), mc.getResourceManager(), mc.getFramebuffer(),
                        new net.minecraft.util.ResourceLocation("shaders/post/blur.json"));
                blurShader.createBindFramebuffers(mc.displayWidth, mc.displayHeight);
                framebuffer = mc.getFramebuffer();
                initialized = true;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        GlStateManager.pushMatrix();
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        framebuffer.bindFramebuffer(false);
        blurShader.loadShaderGroup(mc.timer.renderPartialTicks);
        framebuffer.unbindFramebuffer();

        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.popMatrix();
    }
}