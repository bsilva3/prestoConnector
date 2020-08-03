package helper_classes;

import prestoComm.Constants;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutionException;

public class LoadingScreenAnimator{

    private static JFrame frameGeneralLoading;
    private static JLabel messageLabel;

    public static void openGeneralLoadingAnimation(JFrame frame, String text) {
        frameGeneralLoading = new JFrame(" ");
        frameGeneralLoading.setLayout(new BorderLayout());
        frameGeneralLoading.setUndecorated(true);
        frameGeneralLoading.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
        frameGeneralLoading.setLocationRelativeTo(frame);

        ImageIcon loading = new ImageIcon(Constants.LOADING_GIF);
        //frameGeneralLoading.add(new JLabel(loading, JLabel.CENTER));
        frameGeneralLoading.add(new JLabel(loading),BorderLayout.CENTER);
        messageLabel = new JLabel(text);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        frameGeneralLoading.add(messageLabel, BorderLayout.PAGE_END);

        frameGeneralLoading.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frameGeneralLoading.setSize(400, 200);
        frameGeneralLoading.setVisible(true);
        return;
    }

    public static void setText(String text) {
        messageLabel.setText(text);
    }


    public static void closeGeneralLoadingAnimation() {
        if (frameGeneralLoading != null)
            frameGeneralLoading.dispose();

    }

}