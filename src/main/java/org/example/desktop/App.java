package org.example.desktop;

import org.example.desktop.api.RemoteStorageClient;
import org.example.desktop.impl.DiskRemoteStorage;
import org.example.desktop.ui.LoginDialog;
import org.example.desktop.ui.MainFrame;

import javax.swing.*;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

            // Persistent server folder (override with -Dserver.dir=...)
            String serverDir = System.getProperty("server.dir",
                    System.getProperty("user.home") + "/.light-drive-server");
            RemoteStorageClient client;
            try {
                DiskRemoteStorage disk = new DiskRemoteStorage(Paths.get(serverDir));
                disk.login("demo","demo");
                client = disk;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Cannot init server folder: " + ex.getMessage());
                return;
            }

            LoginDialog login = new LoginDialog(null, client);
            login.setVisible(true);
            if (!login.isOk()) System.exit(0);

            MainFrame frame = new MainFrame(client, login.getUsername());
            frame.setVisible(true);
        });
    }
}