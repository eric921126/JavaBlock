package com.eric.javablock;

import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import java.util.ArrayList;
import java.util.List;

public class PrintBlock extends StackPane {

    private TextField printInput;

    // 【關鍵新增】拖拉與找尾巴專用的變數
    private double dragStartX;
    private double dragStartY;
    private List<javafx.scene.Node> tailBlocks = new ArrayList<>();

    public PrintBlock() {
        // 設定藍色外觀與寬度 (稍微加寬到 280 才能放輸入框)
        Rectangle rect = new Rectangle(280, 40, Color.DODGERBLUE);
        rect.setArcWidth(10);
        rect.setArcHeight(10);

        HBox content = new HBox(5);
        content.setAlignment(Pos.CENTER);

        Text t1 = new Text("System.out.println(");
        t1.setFill(Color.WHITE);
        t1.setFont(Font.font("System", 14));

        printInput = new TextField("\"Hello World\"");
        printInput.setPrefWidth(100);

        Text t2 = new Text(");");
        t2.setFill(Color.WHITE);
        t2.setFont(Font.font("System", 14));

        content.getChildren().addAll(t1, printInput, t2);
        this.getChildren().addAll(rect, content);

        // --- 【關鍵新增】在工作區（Canvas）重複拖拉與邊界防護的滑鼠事件 ---
        this.setOnMousePressed(event -> {
            dragStartX = event.getSceneX() - this.getLayoutX();
            dragStartY = event.getSceneY() - this.getLayoutY();

            tailBlocks.clear();
            javafx.scene.layout.Pane parent = (javafx.scene.layout.Pane) this.getParent();

            if (parent != null) {
                // 純粹計算：安全地把這塊 print 積木下面的整串尾巴找齊
                double currentY = this.getLayoutY();
                double currentHeight = this.getBoundsInParent().getHeight();
                boolean found = true;

                while (found) {
                    found = false;
                    for (javafx.scene.Node node : parent.getChildren()) {
                        if (node != this && !tailBlocks.contains(node)) {
                            // 尋找正下方緊貼著的積木
                            if (Math.abs(node.getLayoutX() - this.getLayoutX()) < 5 &&
                                    Math.abs(node.getLayoutY() - (currentY + currentHeight)) < 5) {

                                tailBlocks.add(node);
                                currentY = node.getLayoutY();
                                currentHeight = node.getBoundsInParent().getHeight();
                                found = true;
                                break;
                            }
                        }
                    }
                }

                // 統一畫面更新：等尾巴都找齊了，再把它們全部浮到最上層
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
                double blockWidth = this.getBoundsInParent().getWidth();
                double blockHeight = this.getBoundsInParent().getHeight();

                // 邊界防護網：限制不能被拖出畫布外
                if (newX < 0) newX = 0;
                if (newY < 0) newY = 0;
                if (newX > parent.getWidth() - blockWidth) newX = parent.getWidth() - blockWidth;
                if (newY > parent.getHeight() - blockHeight) newY = parent.getHeight() - blockHeight;
            }

            // 計算真實移動的位移量，連動底下的尾巴
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

    public String getPrintContent() {
        return printInput.getText();
    }
    public void setPrintContent(String content) {
        if (printInput != null) printInput.setText(content);
    }

    public String getJavaCode() {
        return "System.out.println(" + printInput.getText() + ");";
    }

}