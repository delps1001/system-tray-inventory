package com.delps1001.systemtrayinventory;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.FontType;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.OSType;

import javax.inject.Inject;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Arrays;

@Slf4j
@PluginDescriptor(
        name = "System Tray Inventory Count Plugin"
)
public class SystemTrayInventoryPlugin extends Plugin {
    @Inject
    private Client        client;
    @Inject
    private ClientThread  c;
    @Inject
    private SpriteManager spriteManager;

    private static final int PADDING = 2;

    private TrayIcon trayIcon;

    private static final String TOOLTIP_TEXT_PLACEHOLDER = "%s Inventory Count";

    @Override
    protected void startUp() {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();

            c.invokeLater(() -> {
                trayIcon = new TrayIcon(getImage(0), this.getToolTipText(""), null);
                try {
                    tray.add(trayIcon);
                } catch (AWTException e) {
                    log.error("Error adding tray image", e);
                }
            });
        } else {
            log.warn("System Tray is not supported, OS: {}", OSType.getOSType());
            throw new SystemTrayInventoryPluginException("System not supported");
        }
    }

    private String getToolTipText(String username) {
        return String.format(TOOLTIP_TEXT_PLACEHOLDER, username);
    }

    private Image getImage(int count) {
        BufferedImage image      = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D    graphics2d = image.createGraphics();
        Font          font       = FontType.BOLD.getFont().deriveFont(26f);
        graphics2d.setFont(font);
        FontMetrics fontmetrics = graphics2d.getFontMetrics();
        int         width       = fontmetrics.stringWidth(String.valueOf(count));
        int         height      = fontmetrics.getHeight();
        graphics2d.dispose();

        Image inventorySprite = spriteManager
                .getSprite(SpriteID.TAB_INVENTORY, 0)
                .getScaledInstance(-1, height, Image.SCALE_SMOOTH);

        image = new BufferedImage(inventorySprite.getWidth(null) + PADDING + width, height, BufferedImage.TYPE_INT_ARGB);
        graphics2d = image.createGraphics();
        graphics2d.drawImage(inventorySprite, new AffineTransform(), null);
        graphics2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        graphics2d.setFont(font);
        fontmetrics = graphics2d.getFontMetrics();
        graphics2d.setColor(getColor(count));
        graphics2d.drawString(String.valueOf(count), inventorySprite.getWidth(null) + PADDING, fontmetrics.getAscent());
        graphics2d.dispose();

        return image;
    }

    private Color getColor(int count) {
        if (count == 28) {
            return Color.RED;
        }
        return Color.GRAY;
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged e) {
        if (InventoryID.INVENTORY.getId() == e.getContainerId()) {
            final ItemContainer itemContainer = client.getItemContainer(InventoryID.INVENTORY);
            assert itemContainer != null;
            final Item[] items      = itemContainer.getItems();
            long         totalItems = Arrays.stream(items).filter(i -> i.getQuantity() > 0).count();
            c.invokeLater(() -> this.trayIcon.setImage(getImage((int) totalItems)));
        }
    }

    @Override
    protected void shutDown() {
        log.info("System Tray Inventory Plugin shutting down!");
        if (SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(this.trayIcon);
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN) {
            SystemTray.getSystemTray().remove(this.trayIcon);
        }

        if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
            this.trayIcon.setToolTip(getToolTipText(this.client.getLocalPlayer().getName()));
        }
    }

    @Provides
    SystemTrayInventoryConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(SystemTrayInventoryConfig.class);
    }
}
