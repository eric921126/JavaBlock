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
    private String blockType;

    public DraggableBlock(String label, Color color, String code) {
        this.blockType = label; // 👈 加這行
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

            // 每次點擊時，掃描並記錄掛在自己下面的「整串尾巴」
            tailBlocks.clear();
            javafx.scene.layout.Pane parent = (javafx.scene.layout.Pane) this.getParent();
            if (parent != null) {
                double currentY = this.getLayoutY();
                double currentHeight = this.getBoundsInParent().getHeight();
                boolean found = true;

                while (found) {
                    found = false;
                    for (javafx.scene.Node node : parent.getChildren()) {
                        // 不能找自己，也不能找已經在尾巴清單裡的積木
                        if (node != this && !tailBlocks.contains(node)) {
                            // 條件：X 對齊，且 Y 剛好緊接在下方 (容許 5 像素誤差)
                            if (Math.abs(node.getLayoutX() - this.getLayoutX()) < 5 &&
                                    Math.abs(node.getLayoutY() - (currentY + currentHeight)) < 5) { // 假設積木高度都是 40

                                tailBlocks.add(node);
                                currentY = node.getLayoutY(); // 繼續往下找下一個
                                currentHeight = node.getBoundsInParent().getHeight();
                                found = true;
                                break;
                            }
                        }
                    }
                }
                this.toFront();
                for (javafx.scene.Node tail : tailBlocks) {
                    tail.toFront();
                }
            }
        });

        this.setOnMouseDragged(event -> {
            double newX = event.getSceneX() - dragStartX;
            double newY = event.getSceneY() - dragStartY;

            javafx.scene.layout.Pane parent = (javafx.scene.layout.Pane) this.getParent();
            if (parent != null) {
                // 動態取得目前這塊積木的真實寬高
                double blockWidth = this.getBoundsInParent().getWidth();
                double blockHeight = this.getBoundsInParent().getHeight();

                // 【防護網啟動】如果超出邊界，就強制鎖在邊緣！
                if (newX < 0) newX = 0;
                if (newY < 0) newY = 0;
                if (newX > parent.getWidth() - blockWidth) newX = parent.getWidth() - blockWidth;
                if (newY > parent.getHeight() - blockHeight) newY = parent.getHeight() - blockHeight;
            }

            // 計算最後真正移動了多少距離，讓尾巴跟上
            double deltaX = newX - this.getLayoutX();
            double deltaY = newY - this.getLayoutY();

            this.setLayoutX(newX);
            this.setLayoutY(newY);

            for (javafx.scene.Node tail : tailBlocks) {
                tail.setLayoutX(tail.getLayoutX() + deltaX);
                tail.setLayoutY(tail.getLayoutY() + deltaY);
            }
        });
    }

    public String getJavaCode() {
        return javaCode;
    }

    public String getBlockType() {
        return this.blockType;
    }
}