package com.eric.javablock;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

// 這是一個可以被拖拉的積木物件
public class DraggableBlock extends StackPane {
    private double mouseAnchorX;
    private double mouseAnchorY;
    private String javaCode; // 儲存這塊積木對應的 Java 程式碼
    private double dragStartX;
    private double dragStartY;
    private java.util.List<javafx.scene.Node> tailBlocks = new java.util.ArrayList<>();

    public DraggableBlock(String label, Color color, String code) {
        this.javaCode = code;

        // 1. 繪製積木外觀
        Rectangle rect = new Rectangle(150, 40, color);
        rect.setArcWidth(10);
        rect.setArcHeight(10);
        rect.setStroke(Color.BLACK);

        Text text = new Text(label);
        text.setFill(Color.WHITE);

        this.getChildren().addAll(rect, text);

        // 2. 實作拖拉邏輯
        this.setOnMousePressed(event -> {
            this.toFront();
            dragStartX = event.getSceneX() - this.getLayoutX();
            dragStartY = event.getSceneY() - this.getLayoutY();

            // 魔法核心：每次點擊時，掃描並記錄掛在自己下面的「整串尾巴」
            tailBlocks.clear();
            javafx.scene.layout.Pane parent = (javafx.scene.layout.Pane) this.getParent();
            if (parent != null) {
                double currentY = this.getLayoutY();
                boolean found = true;

                while (found) {
                    found = false;
                    for (javafx.scene.Node node : parent.getChildren()) {
                        // 不能找自己，也不能找已經在尾巴清單裡的積木
                        if (node != this && !tailBlocks.contains(node)) {
                            // 條件：X 對齊，且 Y 剛好緊接在下方 (容許 5 像素誤差)
                            if (Math.abs(node.getLayoutX() - this.getLayoutX()) < 5 &&
                                    Math.abs(node.getLayoutY() - (currentY + 40)) < 5) { // 假設積木高度都是 40

                                tailBlocks.add(node);
                                node.toFront(); // 尾巴也要跟著浮到畫布最上層
                                currentY = node.getLayoutY(); // 繼續往下找下一個
                                found = true;
                                break;
                            }
                        }
                    }
                }
            }
        });

        this.setOnMouseDragged(event -> {
            javafx.scene.layout.Pane parent = (javafx.scene.layout.Pane) this.getParent();
            double newX = event.getSceneX() - dragStartX;
            double newY = event.getSceneY() - dragStartY;

            // 邊界限制 (把你之前寫的防護網加進來，注意 150 和 40 要配合積木大小)
            if (newX < 0) newX = 0;
            if (newY < 0) newY = 0;
            if (newX > parent.getWidth() - 150) newX = parent.getWidth() - 150;
            if (newY > parent.getHeight() - 40) newY = parent.getHeight() - 40;

            // 計算這次滑鼠瞬間移動了多少距離 (Delta)
            double deltaX = newX - this.getLayoutX();
            double deltaY = newY - this.getLayoutY();

            // 移動自己
            this.setLayoutX(newX);
            this.setLayoutY(newY);

            // 【關鍵】讓整串尾巴跟著移動一模一樣的距離！
            for (javafx.scene.Node tail : tailBlocks) {
                tail.setLayoutX(tail.getLayoutX() + deltaX);
                tail.setLayoutY(tail.getLayoutY() + deltaY);
            }
        });
    }

    public String getJavaCode() {
        return javaCode;
    }
}