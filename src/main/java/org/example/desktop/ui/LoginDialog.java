package org.example.desktop.ui;

import org.example.desktop.api.RemoteStorageClient;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class LoginDialog extends JDialog {
    private boolean ok;
    private final JTextField user = new JTextField(16);
    private final JPasswordField pass = new JPasswordField(16);

    public LoginDialog(Frame parent, RemoteStorageClient client) {
        super(parent, "Login", true);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5,5,5,5);
        c.gridx = 0; c.gridy = 0; panel.add(new JLabel("User:"), c);
        c.gridx = 1; panel.add(user, c);
        c.gridx = 0; c.gridy = 1; panel.add(new JLabel("Password:"), c);
        c.gridx = 1; panel.add(pass, c);
        JButton btnOk = new JButton("OK");
        JButton btnCancel = new JButton("Cancel");
        JPanel btns = new JPanel(); btns.add(btnOk); btns.add(btnCancel);
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; panel.add(btns, c);
        setContentPane(panel);
        getRootPane().setDefaultButton(btnOk);
        btnOk.addActionListener(e -> {
            try {
                boolean success = client.login(user.getText(), new String(pass.getPassword()));
                if (success) { ok = true; dispose(); }
                else JOptionPane.showMessageDialog(this, "Login failed", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnCancel.addActionListener(e -> { ok = false; dispose(); });
        pack(); setLocationRelativeTo(parent);
    }
    public boolean isOk() { return ok; }
    public String getUsername() { return user.getText(); }
}
