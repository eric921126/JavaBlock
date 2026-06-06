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

public class VariableUpdateBlock extends StackPane {

    private TextField varNameInput;
    private TextField varValueInput;

    private double dragStartX;
    private double dragStartY;
    private List<javafx.scene.Node> tailBlocks = new ArrayList<>();

    public VariableUpdateBlock() {
        Rectangle rect = new Rectangle(280, 40, Color.CORAL);
        rect.setArcWidth(10);
        rect.setArcHeight(10);

        HBox content = new HBox(5);
        content.setAlignment(Pos.CENTER);

        varNameInput = new TextField("score");
        varNameInput.setPrefWidth(60);

        Text t1 = new Text(" = ");
        t1.setFill(Color.WHITE);
        t1.setFont(Font.font("System", 14));

        varValueInput = new TextField("score + 10");
        varValueInput.setPrefWidth(100);

        Text t2 = new Text(";");
        t2.setFill(Color.WHITE);
        t2.setFont(Font.font("System", 14));

        content.getChildren().addAll(varNameInput, t1, varValueInput, t2);
        this.getChildren().addAll(rect, content);

        // --- 拖拉與找尾巴的滑鼠事件 ---
        this.setOnMousePressed(event -> {
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
                                found = true; break;
                            }
                        }
                    }
                }
                this.toFront();
                for (javafx.scene.Node tail : tailBlocks) { tail.toFront(); }
            }
        });

        this.setOnMouseDragged(event -> {
            double newX = event.getSceneX() - dragStartX;
            double newY = event.getSceneY() - dragStartY;
            javafx.scene.layout.Pane parent = (javafx.scene.layout.Pane) this.getParent();
            if (parent != null) {
                double bW = this.getBoundsInParent().getWidth(); double bH = this.getBoundsInParent().getHeight();
                if (newX < 0) newX = 0; if (newY < 0) newY = 0;
                if (newX > parent.getWidth() - bW) newX = parent.getWidth() - bW;
                if (newY > parent.getHeight() - bH) newY = parent.getHeight() - bH;
            }
            double deltaX = newX - this.getLayoutX(); double deltaY = newY - this.getLayoutY();
            this.setLayoutX(newX); this.setLayoutY(newY);
            for (javafx.scene.Node tail : tailBlocks) {
                tail.setLayoutX(tail.getLayoutX() + deltaX); tail.setLayoutY(tail.getLayoutY() + deltaY);
            }
        });
    }

    public String getVarName() { return varNameInput.getText(); }
    public String getVarValue() { return varValueInput.getText(); }
    public void setVariableUpdateData(String varName, String varValue) {
        if (varNameInput != null) varNameInput.setText(varName);
        if (varValueInput != null) varValueInput.setText(varValue);
    }

    public String getJavaCode() {
        return varNameInput.getText() + " = " + varValueInput.getText() + ";";
    }
}