package vestige.ui.menu;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import vestige.Vestige;
import vestige.font.VestigeFontRenderer;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.lang.reflect.Field;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class AltLoginScreen extends GuiScreen {
    private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
            .setConnectionRequestTimeout(30_000)
            .setConnectTimeout(30_000)
            .setSocketTimeout(30_000)
            .build();
    private static final String CLIENT_ID = "42a60a84-599d-44b2-a7c6-b00cdef1d6a2";
    private static final int PORT = 25575;

    private GuiTextField email;
    private GuiTextField password;
    private final Minecraft mc = Minecraft.getMinecraft();
    private static List<Alt> alts = new ArrayList<>();
    private int selectedAlt = -1;
    private int scrollOffset = 0;
    private VestigeFontRenderer productSans;
    private ResourceLocation backgroundTexture = new ResourceLocation("minecraft", "lycanis/image/background.png");
    private String statusMessage = "";
    private int statusColor = 0xFFFFFF;
    private long statusTime = 0;
    private int mouseX, mouseY;
    private ExecutorService executor = null;
    private CompletableFuture<Void> msAuthTask = null;
    private String msAuthState = null;
    private boolean msAuthSuccess = false;

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        productSans = Vestige.instance.getFontManager().getProductSans();
        ScaledResolution sr = new ScaledResolution(mc);
        int centerX = sr.getScaledWidth() / 2;
        int centerY = sr.getScaledHeight() / 2;

        email = new GuiTextField(0, fontRendererObj, centerX - 150, centerY - 80, 300, 30);
        password = new GuiTextField(1, fontRendererObj, centerX - 150, centerY - 40, 300, 30);
        password.setMaxStringLength(128);
        email.setMaxStringLength(1000);

        buttonList.clear();
        int buttonWidth = 95;
        int buttonHeight = 30;
        int spacing = 10;
        int startX = centerX - (buttonWidth * 3 + spacing * 2) / 2;
        int startY = centerY + 10;

        String[] buttonTexts = {"Login", "Microsoft", "Token", "Copy", "Remove", "Back"};
        String[] buttonIcons = {"login", "back", "import", "copy", "remove", "back"};

        for (int i = 0; i < buttonTexts.length; i++) {
            int row = i / 3;
            int col = i % 3;
            int x = startX + col * (buttonWidth + spacing);
            int y = startY + row * (buttonHeight + spacing);
            buttonList.add(new StyledButton(i, x, y, buttonWidth, buttonHeight, buttonTexts[i], buttonIcons[i]));
        }
    }

    private void drawBlurredBackground(int width, int height) {
        try {
            mc.getTextureManager().bindTexture(backgroundTexture);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            drawModalRectWithCustomSizedTexture(0, 0, 0, 0, width, height, width, height);
        } catch (Exception e) {
            drawRect(0, 0, width, height, 0xFF1a1a1a);
        }

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        GlStateManager.disableTexture2D();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(0, height, 0).color(0.0f, 0.0f, 0.0f, 0.5f).endVertex();
        wr.pos(width, height, 0).color(0.0f, 0.0f, 0.0f, 0.5f).endVertex();
        wr.pos(width, 0, 0).color(0.0f, 0.0f, 0.0f, 0.5f).endVertex();
        wr.pos(0, 0, 0).color(0.0f, 0.0f, 0.0f, 0.5f).endVertex();
        tess.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private void drawMainPanel(int width, int height) {
        float panelWidth = 400;
        float panelHeight = 320;
        float panelX = width / 2f - panelWidth / 2f;
        float panelY = height / 2f - panelHeight / 2f;
        drawRoundedRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 10, new Color(20, 20, 25, 240));
    }

    private void drawAltList(int width, int height) {
        if (alts.isEmpty()) return;

        float listWidth = 200;
        float listHeight = 300;
        float listX = 30;
        float listY = height / 2f - listHeight / 2f;

        drawRoundedRect(listX, listY, listX + listWidth, listY + listHeight, 8, new Color(20, 20, 25, 240));

        productSans.drawStringWithShadow("Recent Alts", listX + 10, listY + 10, 0xFFFFFF);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();
        GL11.glScissor(
                (int)(listX * scale),
                (int)((height - (listY + listHeight - 40)) * scale),
                (int)(listWidth * scale),
                (int)((listHeight - 50) * scale)
        );

        float itemY = listY + 35;
        int itemHeight = 35;
        int visibleStart = Math.max(0, scrollOffset);
        int visibleEnd = Math.min(alts.size(), visibleStart + 7);

        for (int i = visibleStart; i < visibleEnd; i++) {
            Alt alt = alts.get(i);
            float currentY = itemY + (i - scrollOffset) * itemHeight;

            boolean isHovered = mouseX >= listX && mouseX <= listX + listWidth &&
                    mouseY >= currentY && mouseY <= currentY + itemHeight - 5;
            boolean isSelected = i == selectedAlt;

            Color bgColor;
            if (isSelected) {
                bgColor = new Color(60, 60, 80, 220);
            } else if (isHovered) {
                bgColor = new Color(40, 40, 50, 200);
            } else {
                bgColor = new Color(30, 30, 40, 180);
            }

            drawRoundedRect(listX + 5, currentY, listX + listWidth - 5, currentY + itemHeight - 5, 5, bgColor);

            String displayName = alt.username.length() > 18 ? alt.username.substring(0, 16) + ".." : alt.username;
            productSans.drawStringWithShadow(displayName, listX + 12, currentY + 8, 0xFFFFFF);

            String typeText = alt.isPremium ? "Premium" : "Cracked";
            int typeColor = alt.isPremium ? 0x00FF00 : 0xFFAA00;
            productSans.drawString(typeText, listX + 12, currentY + 20, typeColor);
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
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
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        ScaledResolution sr = new ScaledResolution(mc);

        drawBlurredBackground(sr.getScaledWidth(), sr.getScaledHeight());
        drawMainPanel(sr.getScaledWidth(), sr.getScaledHeight());
        drawAltList(sr.getScaledWidth(), sr.getScaledHeight());

        String title = "Alt Manager";
        float titleScale = 1.0f;
        float titleWidth = (float)productSans.getStringWidth(title) * titleScale;
        float titleX = sr.getScaledWidth() / 2.0f - titleWidth / 2.0f;
        float titleY = sr.getScaledHeight() / 2.0f - 140.0f;

        GlStateManager.pushMatrix();
        GlStateManager.translate(titleX, titleY, 0);
        GlStateManager.scale(titleScale, titleScale, 1.0f);
        productSans.drawStringWithShadow(title, 0, 0, 0xFFFFFF);
        GlStateManager.popMatrix();

        productSans.drawStringWithShadow("Email/Username/Token:", width / 2.0f - 150.0f, height / 2.0f - 95.0f, 0xCCCCCC);
        productSans.drawStringWithShadow("Password:", width / 2.0f - 150.0f, height / 2.0f - 55.0f, 0xCCCCCC);

        drawStyledTextField(email);
        drawStyledTextField(password);

        if (System.currentTimeMillis() - statusTime < 3000 && !statusMessage.isEmpty()) {
            float statusWidth = (float)productSans.getStringWidth(statusMessage);
            productSans.drawStringWithShadow(statusMessage,
                    sr.getScaledWidth() / 2.0f - statusWidth / 2.0f,
                    sr.getScaledHeight() / 2.0f + 90.0f,
                    statusColor);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawStyledTextField(GuiTextField textField) {
        if (textField.getVisible()) {
            Color bgColor = textField.isFocused() ? new Color(45, 45, 55, 220) : new Color(35, 35, 45, 200);
            int width = 300;
            int height = 30;
            drawRoundedRect(textField.xPosition - 5, textField.yPosition - 5,
                    textField.xPosition + width + 5, textField.yPosition + height + 5,
                    6, bgColor);
            textField.drawTextBox();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0: loginDirectOrCracked(); break;
            case 1: microsoftLogin(); break;
            case 2: tokenLogin(); break;
            case 3: copySession(); break;
            case 4: removeAlt(); break;
            case 5: mc.displayGuiScreen(new VestigeMainMenu()); break;
        }
    }

    private void setStatus(String message, int color) {
        this.statusMessage = message;
        this.statusColor = color;
        this.statusTime = System.currentTimeMillis();
    }

    private void loginDirectOrCracked() {
        if (selectedAlt != -1 && selectedAlt < alts.size()) {
            Alt targetAlt = alts.get(selectedAlt);
            if (targetAlt.isPremium && !targetAlt.uuid.isEmpty() && !targetAlt.token.isEmpty()) {
                setSession(new Session(targetAlt.username, targetAlt.uuid, targetAlt.token, "mojang"));
                setStatus("Logged in as " + targetAlt.username, 0x00FF00);
            } else {
                setSession(new Session(targetAlt.username, "none", "none", "mojang"));
                setStatus("Cracked login: " + targetAlt.username, 0xFFAA00);
            }
        } else {
            String user = email.getText().trim();
            if (user.isEmpty()) {
                setStatus("Enter username", 0xFF0000);
                return;
            }
            setSession(new Session(user, "none", "none", "mojang"));
            setStatus("Cracked login: " + user, 0xFFAA00);
            addAltIfNew(new Alt(user, false, "", ""));
        }
    }

    private void microsoftLogin() {
        if (executor != null && msAuthTask != null && !msAuthTask.isDone()) {
            setStatus("Already authenticating...", 0xFFAA00);
            return;
        }

        msAuthState = RandomStringUtils.randomAlphanumeric(8);
        URI url = getMSAuthLink(msAuthState);

        if (url != null) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(url.toString()), null);
            setStatus("Link copied! Opening browser...", 0x00FF00);

            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().browse(url);
                } catch (Exception e) {
                    setStatus("Failed to open browser", 0xFF0000);
                }
            }

            if (executor == null) {
                executor = Executors.newSingleThreadExecutor();
            }

            AtomicReference<String> refreshToken = new AtomicReference<>("");
            AtomicReference<String> accessToken = new AtomicReference<>("");

            msAuthTask = acquireMSAuthCode(msAuthState, executor)
                    .thenComposeAsync(msAuthCode -> {
                        setStatus("Acquiring MS tokens", 0xFFAA00);
                        return acquireMSAccessTokens(msAuthCode, executor);
                    })
                    .thenComposeAsync(msAccessTokens -> {
                        setStatus("Acquiring Xbox token", 0xFFAA00);
                        refreshToken.set(msAccessTokens.get("refresh_token"));
                        return acquireXboxAccessToken(msAccessTokens.get("access_token"), executor);
                    })
                    .thenComposeAsync(xboxAccessToken -> {
                        setStatus("Acquiring XSTS token", 0xFFAA00);
                        return acquireXboxXstsToken(xboxAccessToken, executor);
                    })
                    .thenComposeAsync(xboxXstsData -> {
                        setStatus("Acquiring MC token", 0xFFAA00);
                        return acquireMCAccessToken(xboxXstsData.get("Token"), xboxXstsData.get("uhs"), executor);
                    })
                    .thenComposeAsync(mcToken -> {
                        setStatus("Fetching profile", 0xFFAA00);
                        accessToken.set(mcToken);
                        return loginWithMCToken(mcToken, executor);
                    })
                    .thenAccept(session -> {
                        Alt newAlt = new Alt(session.getUsername(), true, session.getPlayerID(), accessToken.get());
                        addAltIfNew(newAlt);
                        setSession(session);
                        setStatus("Logged in: " + session.getUsername(), 0x00FF00);
                        msAuthSuccess = true;
                    })
                    .exceptionally(error -> {
                        setStatus("Auth failed", 0xFF0000);
                        return null;
                    });
        }
    }

    private void tokenLogin() {
        String token = email.getText().trim();

        if (token.isEmpty()) {
            setStatus("Enter token", 0xFF0000);
            return;
        }

        setStatus("Logging in...", 0xFFAA00);

        if (executor == null) {
            executor = Executors.newCachedThreadPool();
        }

        CompletableFuture.runAsync(() -> {
            try {
                Session session = loginWithMCToken(token, executor).get(15, TimeUnit.SECONDS);
                if (session != null) {
                    setSession(session);
                    addAltIfNew(new Alt(session.getUsername(), true, session.getPlayerID(), token));
                    setStatus("Logged in: " + session.getUsername(), 0x00FF00);
                    return;
                }
            } catch (Exception ignored) {}

            try {
                Map<String, String> newTokens = refreshMSAccessToken(token, executor).get(20, TimeUnit.SECONDS);
                String accessToken = newTokens.get("access_token");
                Session session = loginWithMCToken(accessToken, executor).get(15, TimeUnit.SECONDS);
                if (session != null) {
                    setSession(session);
                    addAltIfNew(new Alt(session.getUsername(), true, session.getPlayerID(), accessToken));
                    setStatus("Logged in (refreshed): " + session.getUsername(), 0x00FF00);
                    return;
                }
            } catch (Exception e) {
                setStatus("Token login failed", 0xFF0000);
            }
        }, executor);
    }

    private URI getMSAuthLink(String state) {
        try {
            URIBuilder uriBuilder = new URIBuilder("https://login.live.com/oauth20_authorize.srf")
                    .addParameter("client_id", CLIENT_ID)
                    .addParameter("response_type", "code")
                    .addParameter("redirect_uri", String.format("http://localhost:%d/callback", PORT))
                    .addParameter("scope", "XboxLive.signin XboxLive.offline_access")
                    .addParameter("state", state)
                    .addParameter("prompt", "select_account");
            return uriBuilder.build();
        } catch (Exception e) {
            return null;
        }
    }

    private CompletableFuture<String> acquireMSAuthCode(String state, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<String> authCode = new AtomicReference<>(null);
                AtomicReference<String> errorMsg = new AtomicReference<>(null);

                server.createContext("/callback", exchange -> {
                    Map<String, String> query = URLEncodedUtils
                            .parse(exchange.getRequestURI().toString().replaceAll("/callback\\?", ""), StandardCharsets.UTF_8)
                            .stream()
                            .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));

                    if (!state.equals(query.get("state"))) {
                        errorMsg.set(String.format("State mismatch! Expected '%s' but got '%s'.", state, query.get("state")));
                    } else if (query.containsKey("code")) {
                        authCode.set(query.get("code"));
                    } else if (query.containsKey("error")) {
                        errorMsg.set(String.format("%s: %s", query.get("error"), query.get("error_description")));
                    }

                    String response = "<html><head><style>body{font-family:Arial;text-align:center;padding:50px;background:#1a1a1a;color:#fff;}h1{color:#00ff00;}</style></head><body><h1>Success!</h1><p>You can close this window.</p></body></html>";
                    exchange.getResponseHeaders().add("Content-Type", "text/html");
                    exchange.sendResponseHeaders(200, response.length());
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.getResponseBody().close();

                    latch.countDown();
                });

                try {
                    server.start();
                    latch.await();

                    return Optional.ofNullable(authCode.get())
                            .filter(code -> !StringUtils.isBlank(code))
                            .orElseThrow(() -> new Exception(
                                    Optional.ofNullable(errorMsg.get())
                                            .orElse("No auth code or error description.")
                            ));
                } finally {
                    server.stop(2);
                }
            } catch (InterruptedException e) {
                throw new CancellationException("Microsoft auth code acquisition was cancelled!");
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Microsoft auth code!", e);
            }
        }, executor);
    }

    private CompletableFuture<Map<String, String>> acquireMSAccessTokens(String authCode, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                HttpPost request = new HttpPost(URI.create("https://login.live.com/oauth20_token.srf"));
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Content-Type", "application/x-www-form-urlencoded");
                request.setEntity(new UrlEncodedFormEntity(
                        Arrays.asList(
                                new BasicNameValuePair("client_id", CLIENT_ID),
                                new BasicNameValuePair("grant_type", "authorization_code"),
                                new BasicNameValuePair("code", authCode),
                                new BasicNameValuePair("redirect_uri", String.format("http://localhost:%d/callback", PORT))
                        ),
                        "UTF-8"
                ));

                HttpResponse res = client.execute(request);

                JsonObject json = new JsonParser().parse(EntityUtils.toString(res.getEntity())).getAsJsonObject();
                String accessToken = Optional.ofNullable(json.get("access_token"))
                        .map(JsonElement::getAsString)
                        .filter(token -> !StringUtils.isBlank(token))
                        .orElseThrow(() -> new Exception(json.has("error") ?
                                String.format("%s: %s", json.get("error").getAsString(), json.get("error_description").getAsString()) :
                                "No Microsoft access token."
                        ));
                String refreshToken = Optional.ofNullable(json.get("refresh_token"))
                        .map(JsonElement::getAsString)
                        .filter(token -> !StringUtils.isBlank(token))
                        .orElseThrow(() -> new Exception(json.has("error") ?
                                String.format("%s: %s", json.get("error").getAsString(), json.get("error_description").getAsString()) :
                                "No Microsoft refresh token."
                        ));

                Map<String, String> result = new HashMap<>();
                result.put("access_token", accessToken);
                result.put("refresh_token", refreshToken);
                return result;
            } catch (InterruptedException e) {
                throw new CancellationException("Microsoft access tokens acquisition was cancelled!");
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Microsoft access tokens!", e);
            }
        }, executor);
    }

    private CompletableFuture<String> acquireXboxAccessToken(String accessToken, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                HttpPost request = new HttpPost(URI.create("https://user.auth.xboxlive.com/user/authenticate"));
                JsonObject entity = new JsonObject();
                JsonObject properties = new JsonObject();
                properties.addProperty("AuthMethod", "RPS");
                properties.addProperty("SiteName", "user.auth.xboxlive.com");
                properties.addProperty("RpsTicket", String.format("d=%s", accessToken));
                entity.add("Properties", properties);
                entity.addProperty("RelyingParty", "http://auth.xboxlive.com");
                entity.addProperty("TokenType", "JWT");
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Content-Type", "application/json");
                request.setEntity(new StringEntity(entity.toString()));

                HttpResponse res = client.execute(request);

                JsonObject json = res.getStatusLine().getStatusCode() == 200
                        ? new JsonParser().parse(EntityUtils.toString(res.getEntity())).getAsJsonObject()
                        : new JsonObject();
                return Optional.ofNullable(json.get("Token"))
                        .map(JsonElement::getAsString)
                        .filter(token -> !StringUtils.isBlank(token))
                        .orElseThrow(() -> new Exception(json.has("XErr") ?
                                String.format("%s: %s", json.get("XErr").getAsString(), json.get("Message").getAsString()) :
                                "No Xbox access token."
                        ));
            } catch (InterruptedException e) {
                throw new CancellationException("Xbox Live access token acquisition was cancelled!");
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Xbox Live access token!", e);
            }
        }, executor);
    }

    private CompletableFuture<Map<String, String>> acquireXboxXstsToken(String accessToken, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                HttpPost request = new HttpPost("https://xsts.auth.xboxlive.com/xsts/authorize");
                JsonObject entity = new JsonObject();
                JsonObject properties = new JsonObject();
                JsonArray userTokens = new JsonArray();
                userTokens.add(new JsonPrimitive(accessToken));
                properties.addProperty("SandboxId", "RETAIL");
                properties.add("UserTokens", userTokens);
                entity.add("Properties", properties);
                entity.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
                entity.addProperty("TokenType", "JWT");
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Content-Type", "application/json");
                request.setEntity(new StringEntity(entity.toString()));

                HttpResponse res = client.execute(request);

                JsonObject json = res.getStatusLine().getStatusCode() == 200
                        ? new JsonParser().parse(EntityUtils.toString(res.getEntity())).getAsJsonObject()
                        : new JsonObject();
                return Optional.ofNullable(json.get("Token"))
                        .map(JsonElement::getAsString)
                        .filter(token -> !StringUtils.isBlank(token))
                        .map(token -> {
                            String uhs = json.get("DisplayClaims").getAsJsonObject()
                                    .get("xui").getAsJsonArray()
                                    .get(0).getAsJsonObject()
                                    .get("uhs").getAsString();

                            Map<String, String> result = new HashMap<>();
                            result.put("Token", token);
                            result.put("uhs", uhs);
                            return result;
                        })
                        .orElseThrow(() -> new Exception(json.has("XErr") ?
                                String.format("%s: %s", json.get("XErr").getAsString(), json.get("Message").getAsString()) :
                                "No XSTS token."
                        ));
            } catch (InterruptedException e) {
                throw new CancellationException("Xbox Live XSTS token acquisition was cancelled!");
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Xbox Live XSTS token!", e);
            }
        }, executor);
    }

    private CompletableFuture<String> acquireMCAccessToken(String xstsToken, String userHash, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                HttpPost request = new HttpPost(URI.create("https://api.minecraftservices.com/authentication/login_with_xbox"));
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Content-Type", "application/json");
                request.setEntity(new StringEntity(
                        String.format("{\"identityToken\": \"XBL3.0 x=%s;%s\"}", userHash, xstsToken)
                ));

                HttpResponse res = client.execute(request);

                JsonObject json = new JsonParser().parse(EntityUtils.toString(res.getEntity())).getAsJsonObject();

                return Optional.ofNullable(json.get("access_token"))
                        .map(JsonElement::getAsString)
                        .filter(token -> !StringUtils.isBlank(token))
                        .orElseThrow(() -> new Exception(json.has("error") ?
                                String.format("%s: %s", json.get("error").getAsString(), json.get("errorMessage").getAsString()) :
                                "No Minecraft access token."
                        ));
            } catch (InterruptedException e) {
                throw new CancellationException("Minecraft access token acquisition was cancelled!");
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Minecraft access token!", e);
            }
        }, executor);
    }

    private CompletableFuture<Session> loginWithMCToken(String mcToken, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                HttpGet request = new HttpGet(URI.create("https://api.minecraftservices.com/minecraft/profile"));
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Authorization", "Bearer " + mcToken);

                HttpResponse res = client.execute(request);

                JsonObject json = new JsonParser().parse(EntityUtils.toString(res.getEntity())).getAsJsonObject();
                return Optional.ofNullable(json.get("id"))
                        .map(JsonElement::getAsString)
                        .filter(uuid -> !StringUtils.isBlank(uuid))
                        .map(uuid -> new Session(
                                json.get("name").getAsString(),
                                uuid,
                                mcToken,
                                "mojang"
                        ))
                        .orElseThrow(() -> new Exception(json.has("error") ?
                                String.format("%s: %s", json.get("error").getAsString(), json.get("errorMessage").getAsString()) :
                                "No profile."
                        ));
            } catch (InterruptedException e) {
                throw new CancellationException("Minecraft profile fetching was cancelled!");
            } catch (Exception e) {
                throw new CompletionException("Unable to fetch Minecraft profile!", e);
            }
        }, executor);
    }

    private void addAltIfNew(Alt alt) {
        for (Alt existing : alts) {
            if (existing.username.equals(alt.username)) {
                if (alt.isPremium) {
                    existing.isPremium = true;
                    existing.uuid = alt.uuid;
                    existing.token = alt.token;
                }
                return;
            }
        }
        alts.add(0, alt);
        if (alts.size() > 20) {
            alts.remove(alts.size() - 1);
        }
    }

    private void removeAlt() {
        if (selectedAlt != -1 && selectedAlt < alts.size()) {
            alts.remove(selectedAlt);
            selectedAlt = -1;
            setStatus("Alt removed", 0xFFAA00);
        }
    }

    private void copySession() {
        try {
            Session session = mc.getSession();
            String sessionString = session.getUsername() + ":" + session.getPlayerID() + ":" + session.getToken();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sessionString), null);
            setStatus("Session copied", 0x00FF00);
        } catch (Exception e) {
            setStatus("Copy failed", 0xFF0000);
        }
    }

    private void setSession(Session session) {
        try {
            Field sessionField = Minecraft.class.getDeclaredField("session");
            sessionField.setAccessible(true);
            sessionField.set(mc, session);
        } catch (Exception e) {
            setStatus("Session set failed", 0xFF0000);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        email.textboxKeyTyped(typedChar, keyCode);
        password.textboxKeyTyped(typedChar, keyCode);

        if (keyCode == Keyboard.KEY_RETURN) {
            if (password.isFocused() && !password.getText().isEmpty()) {
                loginDirectOrCracked();
            } else if (email.isFocused()) {
                password.setFocused(true);
                email.setFocused(false);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        email.mouseClicked(mouseX, mouseY, mouseButton);
        password.mouseClicked(mouseX, mouseY, mouseButton);

        if (!alts.isEmpty()) {
            float listWidth = 200;
            float listHeight = 300;
            float listX = 30;
            float listY = height / 2f - listHeight / 2f;

            if (mouseX >= listX && mouseX <= listX + listWidth &&
                    mouseY >= listY + 35 && mouseY <= listY + listHeight) {

                int clickedIndex = (int)((mouseY - (listY + 35)) / 35) + scrollOffset;
                if (clickedIndex >= 0 && clickedIndex < alts.size()) {
                    if (mouseButton == 0) {
                        selectedAlt = clickedIndex;
                        Alt alt = alts.get(selectedAlt);
                        if (alt.isPremium && !alt.uuid.isEmpty() && !alt.token.isEmpty()) {
                            setSession(new Session(alt.username, alt.uuid, alt.token, "mojang"));
                            setStatus("Logged in: " + alt.username, 0x00FF00);
                        } else {
                            setSession(new Session(alt.username, "none", "none", "mojang"));
                            setStatus("Cracked: " + alt.username, 0xFFAA00);
                        }
                    } else if (mouseButton == 1) {
                        selectedAlt = clickedIndex;
                    }
                }
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (wheel != 0 && !alts.isEmpty()) {
            if (wheel > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else {
                scrollOffset = Math.min(Math.max(0, alts.size() - 7), scrollOffset + 1);
            }
        }
    }

    @Override
    public void updateScreen() {
        email.updateCursorCounter();
        password.updateCursorCounter();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        if (msAuthTask != null && !msAuthTask.isDone()) {
            msAuthTask.cancel(true);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private class StyledButton extends GuiButton {
        private float hoverAnimation = 0.0f;
        private String iconName;
        private ResourceLocation iconTexture;

        public StyledButton(int id, int x, int y, int width, int height, String text, String iconName) {
            super(id, x, y, width, height, text);
            this.iconName = iconName;
            this.iconTexture = new ResourceLocation("minecraft", "lycanis/image/" + iconName + ".png");
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (this.visible) {
                boolean hovered = mouseX >= this.xPosition && mouseY >= this.yPosition &&
                        mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

                if (hovered) hoverAnimation = Math.min(1.0f, hoverAnimation + 0.08f);
                else hoverAnimation = Math.max(0.0f, hoverAnimation - 0.08f);

                float scale = 1.05f - 0.05f * (1.0f - hoverAnimation);
                GlStateManager.pushMatrix();
                GlStateManager.translate(this.xPosition + this.width / 2f, this.yPosition + this.height / 2f, 0);
                GlStateManager.scale(scale, scale, 1.0f);
                GlStateManager.translate(-(this.xPosition + this.width / 2f), -(this.yPosition + this.height / 2f), 0);

                Color bgColor = hovered ? new Color(45, 45, 55, 220) : new Color(30, 30, 40, 200);
                AltLoginScreen.this.drawRoundedRect(this.xPosition, this.yPosition,
                        this.xPosition + this.width, this.yPosition + this.height, 6, bgColor);

                int textColor = hovered ? 0xFFFFFF : 0xCCCCCC;
                int textX = this.xPosition + (int)((this.width - productSans.getStringWidth(this.displayString)) / 2);
                int textY = this.yPosition + (this.height - 8) / 2;

                productSans.drawStringWithShadow(this.displayString, textX, textY, textColor);
                GlStateManager.popMatrix();
            }
        }
    }

    private CompletableFuture<Map<String, String>> refreshMSAccessToken(String refreshToken, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                HttpPost request = new HttpPost(URI.create("https://login.live.com/oauth20_token.srf"));
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Content-Type", "application/x-www-form-urlencoded");
                request.setEntity(new UrlEncodedFormEntity(
                        Arrays.asList(
                                new BasicNameValuePair("client_id", CLIENT_ID),
                                new BasicNameValuePair("grant_type", "refresh_token"),
                                new BasicNameValuePair("refresh_token", refreshToken),
                                new BasicNameValuePair("redirect_uri", String.format("http://localhost:%d/callback", PORT))
                        ),
                        "UTF-8"
                ));

                HttpResponse res = client.execute(request);
                JsonObject json = new JsonParser().parse(EntityUtils.toString(res.getEntity())).getAsJsonObject();

                String newAccess = json.get("access_token").getAsString();
                String newRefresh = json.get("refresh_token").getAsString();

                Map<String, String> result = new HashMap<>();
                result.put("access_token", newAccess);
                result.put("refresh_token", newRefresh);
                return result;
            } catch (Exception e) {
                throw new CompletionException("Failed to refresh token", e);
            }
        }, executor);
    }


    private static class Alt {
        public String username;
        public boolean isPremium;
        public String uuid;
        public String token;

        public Alt(String username, boolean isPremium, String uuid, String token) {
            this.username = username;
            this.isPremium = isPremium;
            this.uuid = uuid;
            this.token = token;
        }
    }
}