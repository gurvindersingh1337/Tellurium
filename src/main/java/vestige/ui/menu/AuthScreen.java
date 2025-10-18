package vestige.ui.menu;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import vestige.ui.menu.VestigeMainMenu;
import java.io.*;
import java.util.Random;

public class AuthScreen extends GuiScreen {

    private GuiTextField licenseField;
    private GuiButton activateButton;
    private String status = "";
    private boolean waiting = false;
    private long targetTime = 0;
    private final String[] VALID_LICENSES = {
            "TELLURIUM-F116-1286-8261",
            "TELLURIUM-F881-1946-7151"
    };
    private final Random random = new Random();
    private float fade = 0;
    private float fieldAlpha = 0;

    private static final File AUTH_FILE = new File("tellurium_auth.dat");

    public static boolean isAuthenticated() {
        if (!AUTH_FILE.exists()) return false;
        try (BufferedReader reader = new BufferedReader(new FileReader(AUTH_FILE))) {
            String saved = reader.readLine();
            return saved != null && saved.equals("authenticated");
        } catch (IOException e) {
            return false;
        }
    }

    private void saveAuthentication() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(AUTH_FILE))) {
            writer.write("authenticated");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initGui() {
        if (isAuthenticated()) {
            mc.displayGuiScreen(new VestigeMainMenu());
            return;
        }

        licenseField = new GuiTextField(0, mc.fontRendererObj, width / 2 - 100, height / 2 - 10, 200, 20);
        licenseField.setMaxStringLength(32);
        licenseField.setFocused(true);

        buttonList.clear();
        activateButton = new GuiButton(0, width / 2 - 50, height / 2 + 20, 100, 20, "Activate");
        buttonList.add(activateButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();
        int screenHeight = sr.getScaledHeight();

        fade = Math.min(fade + 0.05f, 1.0f);
        fieldAlpha = Math.min(fieldAlpha + 0.03f, 1.0f);

        drawGradientRect(0, 0, width, height, 0xFF0a0a0a, 0xFF1a1a1a);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();

        float scale = 2.0f;
        GlStateManager.scale(scale, scale, 1);
        int titleColor = interpolateColor(0x6096ff, 0x60c8ff, (float) Math.sin(System.currentTimeMillis() / 1000.0) * 0.5f + 0.5f);
        drawCenteredString(mc.fontRendererObj, "Tellurium", (int)(screenWidth / scale / 2), (int)((height / 2 - 60) / scale), titleColor);
        GlStateManager.popMatrix();

        int subtitleY = height / 2 - 35;
        drawCenteredString(mc.fontRendererObj, "License Activation", width / 2, subtitleY, applyAlpha(0x999999, fade));

        GlStateManager.pushMatrix();
        GlStateManager.color(1, 1, 1, fieldAlpha);
        licenseField.drawTextBox();
        GlStateManager.popMatrix();

        if (!status.isEmpty()) {
            int color;
            if (status.equals("Activation successful!") || status.equals("Logging in...")) {
                color = applyAlpha(0x00ff88, fade);
            } else {
                color = applyAlpha(0xff4444, fade);
            }
            drawCenteredString(mc.fontRendererObj, status, width / 2, height / 2 + 50, color);
        }

        if (waiting && System.currentTimeMillis() >= targetTime) {
            saveAuthentication();
            mc.displayGuiScreen(new VestigeMainMenu());
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (!waiting) {
            licenseField.textboxKeyTyped(typedChar, keyCode);
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (!waiting) {
            licenseField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == activateButton && !waiting) {
            boolean valid = false;
            for (String key : VALID_LICENSES) {
                if (licenseField.getText().equals(key)) {
                    valid = true;
                    break;
                }
            }
            if (valid) {
                status = "Activation successful!";
                waiting = true;
                int delay = 1000 + random.nextInt(2000);
                targetTime = System.currentTimeMillis() + delay;
                status = "Logging in...";
            } else {
                status = "Invalid license key!";
            }
        }
    }

    private int applyAlpha(int color, float alpha) {
        int a = (int)(alpha * 255);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int interpolateColor(int color1, int color2, float ratio) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int r = (int)(r1 + (r2 - r1) * ratio);
        int g = (int)(g1 + (g2 - g1) * ratio);
        int b = (int)(b1 + (b2 - b1) * ratio);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}