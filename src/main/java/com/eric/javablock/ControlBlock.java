package com.eric.javablock;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.layout.StackPane;
import java.util.ArrayList;
import java.util.List;

public class ControlBlock extends VBox {

    public VBox innerContainer;
    private String blockType;

    private TextField forInit;
    private TextField forCondition;
    private TextField forStep;
    // 【新增】Class 類別專用輸入框
    private TextField classNameInput;
    // 【新增】Method 方法專用輸入框
    private TextField methodNameInput;
    private TextField methodParamsInput;

    private double dragStartX;
    private double dragStartY;
    private List<javafx.scene.Node> tailBlocks = new ArrayList<>();

    public ControlBlock(String type) {
        this.blockType = type;
        this.setSpacing(0);
        this.setFillWidth(false);

        // 【設定色彩】class 用紫色，if 用金色，其餘迴圈用綠色
        Color blockColor = Color.MEDIUMSEAGREEN;
        String borderHex = "#3CB371";

        if (type.equals("class")) {
            blockColor = Color.PURPLE;
            borderHex = "#800080";
        } else if (type.equals("method")) {
            blockColor = Color.MEDIUMPURPLE;
            borderHex = "#9370DB"; // 淺紫色邊框
        } else if (type.equals("if")) {
            blockColor = Color.GOLD;
            borderHex = "#FFD700";
        }

        double blockWidth = type.equals("for") ? 350 : (type.equals("method") ? 320 : 200);

        StackPane header;
        if (type.equals("for")) {
            header = createForHeader(blockWidth, blockColor);
        } else if (type.equals("class")) {
            header = createClassHeader(blockWidth, blockColor);
        } else if (type.equals("method")) {
            // 【新增】呼叫帶有輸入框的 method 頭部
            header = createMethodHeader(blockWidth, blockColor);
        } else if (type.equals("if")) {
            header = createBar("if ( condition ) {", blockWidth, blockColor);
        } else {
            header = createBar(type + " ( true ) {", blockWidth, blockColor);
        }

        innerContainer = new VBox();
        innerContainer.setSpacing(0);
        innerContainer.setPadding(new Insets(5, 0, 5, 5));
        innerContainer.setMinWidth(blockWidth);
        innerContainer.setStyle("-fx-border-color: " + borderHex + "; -fx-border-width: 0 0 0 15;");
        innerContainer.setMinHeight(40); // 類別的肚子預設給大一點，方便塞方法

        // 【關鍵修改 2】動態調整脊椎邊框的顏色
        innerContainer.setStyle("-fx-border-color: " + borderHex + "; -fx-border-width: 0 0 0 15;");
        innerContainer.setMinHeight(30);

        StackPane footer = createBar("}", blockWidth, blockColor);

        this.getChildren().addAll(header, innerContainer, footer);

        // 拖拉與找尾巴邏輯
        this.setOnMousePressed(event -> {
            this.toFront();
            dragStartX = event.getSceneX() - this.getLayoutX();
            dragStartY = event.getSceneY() - this.getLayoutY();

            tailBlocks.clear();
            javafx.scene.layout.Pane parent = (javafx.scene.layout.Pane) this.getParent();

            if (parent != null) {
                double currentY = this.getLayoutY();
                double currentHeight = this.getBoundsInParent().getHeight();
                boolean found = true;

                while (found) {
                    found = false;
                    for (javafx.scene.Node node : parent.getChildren()) {
                        if (node != this && !tailBlocks.contains(node)) {
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
                double blockwidth = this.getBoundsInParent().getWidth();
                double blockHeight = this.getBoundsInParent().getHeight();

                // 【防護網啟動】如果超出邊界，就強制鎖在邊緣！
                if (newX < 0) newX = 0;
                if (newY < 0) newY = 0;
                if (newX > parent.getWidth() - blockwidth) newX = parent.getWidth() - blockwidth;
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

    private StackPane createBar(String label, double width, Color color) {
        StackPane bar = new StackPane();
        Rectangle rect = new Rectangle(width, 35, color);
        rect.setArcWidth(10); rect.setArcHeight(10);

        Text text = new Text(label);
        // 如果是金色積木，字體用黑色比較清晰；綠色用白色
        text.setFill(color == Color.GOLD ? Color.BLACK : Color.WHITE);
        text.setFont(Font.font("System", 14));

        bar.getChildren().addAll(rect, text);
        return bar;
    }

    private StackPane createForHeader(double width, Color color) {
        StackPane bar = new StackPane();
        Rectangle rect = new Rectangle(width, 35, color);
        rect.setArcWidth(10); rect.setArcHeight(10);

        HBox content = new HBox(5);
        content.setAlignment(Pos.CENTER);

        Text t1 = new Text("for ("); t1.setFill(Color.WHITE); t1.setFont(Font.font("System", 14));
        forInit = new TextField("int i = 0"); forInit.setPrefWidth(75);
        Text t2 = new Text(";"); t2.setFill(Color.WHITE); t2.setFont(Font.font("System", 14));
        forCondition = new TextField("i < 5"); forCondition.setPrefWidth(55);
        Text t3 = new Text(";"); t3.setFill(Color.WHITE); t3.setFont(Font.font("System", 14));
        forStep = new TextField("i++"); forStep.setPrefWidth(45);
        Text t4 = new Text(") {"); t4.setFill(Color.WHITE); t4.setFont(Font.font("System", 14));

        content.getChildren().addAll(t1, forInit, t2, forCondition, t3, forStep, t4);
        bar.getChildren().addAll(rect, content);
        return bar;
    }

    // 【新增】產生帶有輸入框的 class 頭部 UI
    private StackPane createClassHeader(double width, Color color) {
        StackPane bar = new StackPane();
        Rectangle rect = new Rectangle(width, 35, color);
        rect.setArcWidth(10); rect.setArcHeight(10);

        HBox content = new HBox(5);
        content.setAlignment(Pos.CENTER);

        Text t1 = new Text("public class ");
        t1.setFill(Color.WHITE);
        t1.setFont(Font.font("System", 14));

        classNameInput = new TextField("MyClass");
        classNameInput.setPrefWidth(80);

        Text t2 = new Text(" {");
        t2.setFill(Color.WHITE);
        t2.setFont(Font.font("System", 14));

        content.getChildren().addAll(t1, classNameInput, t2);
        bar.getChildren().addAll(rect, content);
        return bar;
    }

    // 【新增】產生帶有輸入框的 method 頭部 UI
    private StackPane createMethodHeader(double width, Color color) {
        StackPane bar = new StackPane();
        Rectangle rect = new Rectangle(width, 35, color);
        rect.setArcWidth(10); rect.setArcHeight(10);
        HBox content = new HBox(5); content.setAlignment(Pos.CENTER);

        Text t1 = new Text("public void ");
        t1.setFill(Color.WHITE);
        t1.setFont(Font.font("System", 14));

        methodNameInput = new TextField("myMethod");
        methodNameInput.setPrefWidth(80);

        Text t2 = new Text("(");
        t2.setFill(Color.WHITE);
        t2.setFont(Font.font("System", 14));

        // 【新增】參數輸入框
        methodParamsInput = new TextField("");
        methodParamsInput.setPromptText("int x"); // 水印提示字
        methodParamsInput.setPrefWidth(80);

        Text t3 = new Text(") {");
        t3.setFill(Color.WHITE);
        t3.setFont(Font.font("System", 14));

        content.getChildren().addAll(t1, methodNameInput, t2, methodParamsInput, t3);
        bar.getChildren().addAll(rect, content);
        return bar;
    }

    public String getJavaCode() {
        StringBuilder sb = new StringBuilder();

        if (blockType.equals("class")) {
            sb.append("public class ").append(classNameInput.getText()).append(" {\n");
        } else if (blockType.equals("method")) {
            sb.append("public void ").append(methodNameInput.getText())
                    .append("(").append(methodParamsInput.getText()).append(") {\n");
        } else if (blockType.equals("for")) {
            sb.append("for (").append(forInit.getText()).append("; ")
                    .append(forCondition.getText()).append("; ")
                    .append(forStep.getText()).append(") {\n");
        } else if (blockType.equals("if")) {
            sb.append("if (true) {\n");
        } else {
            sb.append(blockType).append(" (true) {\n");
        }

        for (javafx.scene.Node node : innerContainer.getChildren()) {
            String childCode = "";

            if (node instanceof DraggableBlock) childCode = ((DraggableBlock) node).getJavaCode();
            else if (node instanceof VariableBlock) childCode = ((VariableBlock) node).getJavaCode();
            else if (node instanceof ControlBlock) childCode = ((ControlBlock) node).getJavaCode();
            else if (node instanceof PrintBlock) childCode = ((PrintBlock) node).getJavaCode();
            else if (node instanceof NewObjectBlock) childCode = ((NewObjectBlock) node).getJavaCode();
            else if (node instanceof MethodCallBlock) childCode = ((MethodCallBlock) node).getJavaCode();
            else if (node instanceof VariableUpdateBlock) childCode = ((VariableUpdateBlock) node).getJavaCode();
            else if (node instanceof ReturnBlock) childCode = ((ReturnBlock) node).getJavaCode();
            if (!childCode.isEmpty()) {
                sb.append(childCode.replaceAll("(?m)^", "    ")).append("\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    public String getBlockType() {
        return this.blockType;
    }

}