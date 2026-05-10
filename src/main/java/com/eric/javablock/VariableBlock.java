package com.eric.javablock;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

// 這是一個專門用來宣告變數的動態積木 (支援多型別與防呆)
public class VariableBlock extends StackPane {
    private ComboBox<String> typeSelector; // 型別下拉選單
    private TextField nameField;
    private TextField valueField;
    private double dragStartX;
    private double dragStartY;
    private java.util.List<javafx.scene.Node> tailBlocks = new java.util.ArrayList<>();

    public VariableBlock() {
        // 1. 背景加長一點，因為下拉選單比較佔空間 (從 200 改成 280)
        Rectangle rect = new Rectangle(280, 40, Color.DARKORANGE);
        rect.setArcWidth(10);
        rect.setArcHeight(10);
        rect.setStroke(Color.BLACK);

        HBox content = new HBox(5);
        content.setAlignment(Pos.CENTER);

        // 2. 建立下拉式選單，讓使用者選型別
        typeSelector = new ComboBox<>(FXCollections.observableArrayList(
                "int", "double", "boolean", "String"
        ));
        typeSelector.setValue("int"); // 預設是 int
        typeSelector.setPrefWidth(85);
        typeSelector.setStyle("-fx-font-size: 12px; -fx-padding: 0;"); // 讓選單小一點比較好看

        nameField = new TextField("x");
        nameField.setPrefWidth(40);

        Text equalsText = new Text(" = ");
        equalsText.setFill(Color.WHITE);
        equalsText.setFont(Font.font("System", 14));

        valueField = new TextField("10");
        valueField.setPrefWidth(60);

        content.getChildren().addAll(typeSelector, nameField, equalsText, valueField);
        this.getChildren().addAll(rect, content);

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

            // 邊界限制
            if (newX < 0) newX = 0;
            if (newY < 0) newY = 0;
            if (newX > parent.getWidth() - 280) newX = parent.getWidth() - 280;
            if (newY > parent.getHeight() - 40) newY = parent.getHeight() - 40;

            // 計算這次滑鼠瞬間移動了多少距離 (Delta)
            double deltaX = newX - this.getLayoutX();
            double deltaY = newY - this.getLayoutY();

            // 移動自己
            this.setLayoutX(newX);
            this.setLayoutY(newY);

            for (javafx.scene.Node tail : tailBlocks) {
                tail.setLayoutX(tail.getLayoutX() + deltaX);
                tail.setLayoutY(tail.getLayoutY() + deltaY);
            }
        });

        // 4. 【核心防呆機制】監聽輸入框和下拉選單的改變
        valueField.textProperty().addListener((observable, oldValue, newValue) -> validateInput());
        typeSelector.valueProperty().addListener((observable, oldValue, newValue) -> validateInput());
    }

    // 檢查輸入是否合法的防呆小程式
    private void validateInput() {
        String type = typeSelector.getValue();
        String value = valueField.getText();
        boolean isValid = true;

        try {
            switch (type) {
                case "int":
                    Integer.parseInt(value); // 測試能不能轉成整數
                    break;
                case "double":
                    Double.parseDouble(value); // 測試能不能轉成小數
                    break;
                case "boolean":
                    if (!value.equals("true") && !value.equals("false")) {
                        isValid = false;
                    }
                    break;
                case "String":
                    // 字串什麼都可以裝，所以絕對合法
                    break;
            }
        } catch (NumberFormatException e) {
            // 如果轉換失敗（例如 int 塞了 0.9 或 abc），程式會跳來這裡
            isValid = false;
        }

        // 如果不合法，把輸入框背景變成「淺紅色」警告使用者！
        if (isValid) {
            valueField.setStyle("-fx-control-inner-background: white;");
        } else {
            valueField.setStyle("-fx-control-inner-background: #FFCCCC;");
        }
    }

    // 轉換成 Java 程式碼
    public String getJavaCode() {
        String type = typeSelector.getValue();
        String name = nameField.getText();
        String value = valueField.getText();

        // 【神奇小細節】如果是 String，程式碼要自動幫它加上雙引號！
        if (type.equals("String")) {
            value = "\"" + value + "\"";
        }

        return type + " " + name + " = " + value + ";";
    }
}