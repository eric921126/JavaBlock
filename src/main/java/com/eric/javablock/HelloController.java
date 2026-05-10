package com.eric.javablock;

import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

public class HelloController {

    @FXML
    private VBox paletteBox; // 左邊的工具箱

    @FXML
    private Pane workspace; // 中間的畫布

    @FXML
    private TextArea codePreview; // 右邊的程式碼預覽

    private void createVariablePaletteItem() {
        StackPane item = new StackPane();
        Rectangle rect = new Rectangle(150, 40, Color.DARKORANGE);
        rect.setArcWidth(10);
        rect.setArcHeight(10);
        Text text = new Text("Create Variable");
        text.setFill(Color.WHITE);
        item.getChildren().addAll(rect, text);

        paletteBox.getChildren().add(item);

        item.setOnMousePressed(event -> {
            // 這裡召喚的是有輸入框的 VariableBlock
            VariableBlock realBlock = new VariableBlock();
            workspace.getChildren().add(realBlock);

            item.setOnMouseDragged(dragEvent -> {
                javafx.geometry.Point2D localPoint = workspace.sceneToLocal(dragEvent.getSceneX(), dragEvent.getSceneY());
                double newX = localPoint.getX() - 140; // 150的一半
                double newY = localPoint.getY() - 20; // 40的一半

                // 邊界限制：不准小於 0，也不准大於畫布極限
                if (newX < 0) newX = 0;
                if (newY < 0) newY = 0;
                if (newX > workspace.getWidth() - 280) newX = workspace.getWidth() - 150;
                if (newY > workspace.getHeight() - 40) newY = workspace.getHeight() - 40;

                realBlock.setLayoutX(newX);
                realBlock.setLayoutY(newY);
            });

            item.setOnMouseReleased(releaseEvent -> {
                item.setOnMouseDragged(null);
                handleSnapping(realBlock);
                updateCodeArea();
            });

            realBlock.setOnMouseReleased(releaseEvent -> {
                handleSnapping(realBlock);
                updateCodeArea();
            });

            // 【新增功能：右鍵刪除選單】
            javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
            javafx.scene.control.MenuItem deleteItem = new javafx.scene.control.MenuItem("🗑️ 刪除此積木");

            // 設定點擊「刪除」後要發生的事
            deleteItem.setOnAction(e -> {
                // 1. 從畫布中拔除這塊積木
                workspace.getChildren().remove(realBlock);
                // 2. 重新整理右邊的程式碼
                updateCodeArea();
            });

            contextMenu.getItems().add(deleteItem);

            // 告訴積木：當有人對你按右鍵 (ContextMenuRequested) 時，就把選單秀出來
            realBlock.setOnContextMenuRequested(e -> {
                contextMenu.show(realBlock, event.getScreenX(), event.getScreenY());
            });

            // 當你在輸入框打字時，即時更新程式碼
            realBlock.setOnKeyReleased(keyEvent -> updateCodeArea());

            // 當你用滑鼠點擊下拉選單換型別時，也即時更新程式碼
            realBlock.setOnMouseClicked(mouseEvent -> updateCodeArea());
        });
    }

    // initialize 是 JavaFX 視窗打開時會「自動執行」的第一個方法
    // initialize 是 JavaFX 視窗打開時會「自動執行」的第一個方法
    @FXML
    public void initialize() {
        // 1. 輸出/輸入類 (藍色)
        createPaletteItem("System.out.println", Color.DODGERBLUE, "System.out.println(\"Hello World\");");

        // 2. 變數宣告類 (橘色)
        createVariablePaletteItem();

        // 3. 條件判斷類 (金色/黃色)
        createPaletteItem("if ( condition )", Color.GOLD, "if (true) {\n    // TODO: 條件成立執行\n}");

        // 4. 迴圈控制類 (綠色)
        createPaletteItem("for loop", Color.MEDIUMSEAGREEN, "for (int i = 0; i < 5; i++) {\n    // TODO: 迴圈內容\n}");
        createPaletteItem("while loop", Color.MEDIUMSEAGREEN, "while (true) {\n    // TODO: 迴圈內容\n}");
    }

    // 這是一個用來製造「工具箱積木模型」的方法
    private void createPaletteItem(String label, Color color, String code) {
        // 1. 畫出工具箱裡的假積木 (外觀跟真的一樣)
        StackPane item = new StackPane();
        Rectangle rect = new Rectangle(150, 40, color);
        rect.setArcWidth(10);
        rect.setArcHeight(10);
        Text text = new Text(label);
        text.setFill(Color.WHITE);
        item.getChildren().addAll(rect, text);

        // 2. 把它加到左邊的 VBox 工具箱裡
        paletteBox.getChildren().add(item);

        // 3. 神奇的拖拉魔法：當你在工具箱的積木上按下左鍵
        item.setOnMousePressed(event -> {
            // 立刻在右邊畫布「召喚」一個真正的、可以拖拉的積木
            DraggableBlock realBlock = new DraggableBlock(label, color, code);
            workspace.getChildren().add(realBlock);

            // 當你按住滑鼠不放並移動時...
            item.setOnMouseDragged(dragEvent -> {
                javafx.geometry.Point2D localPoint = workspace.sceneToLocal(dragEvent.getSceneX(), dragEvent.getSceneY());
                double newX = localPoint.getX() - 75; // 150的一半
                double newY = localPoint.getY() - 20; // 40的一半

                // 邊界限制：不准小於 0，也不准大於畫布極限
                if (newX < 0) newX = 0;
                if (newY < 0) newY = 0;
                if (newX > workspace.getWidth() - 150) newX = workspace.getWidth() - 150;
                if (newY > workspace.getHeight() - 40) newY = workspace.getHeight() - 40;

                realBlock.setLayoutX(newX);
                realBlock.setLayoutY(newY);
            });

            // 當你放開滑鼠時，切斷工具箱跟滑鼠的連動，更新程式碼
            item.setOnMouseReleased(releaseEvent -> {
                item.setOnMouseDragged(null);
                handleSnapping(realBlock);
                updateCodeArea();
            });

            realBlock.setOnMouseReleased(releaseEvent -> {
                handleSnapping(realBlock);
                updateCodeArea();
            });

            // 【新增功能：右鍵刪除選單】
            javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
            javafx.scene.control.MenuItem deleteItem = new javafx.scene.control.MenuItem("🗑️ 刪除此積木");

            // 設定點擊「刪除」後要發生的事
            deleteItem.setOnAction(e -> {
                // 1. 從畫布中拔除這塊積木
                workspace.getChildren().remove(realBlock);
                // 2. 重新整理右邊的程式碼
                updateCodeArea();
            });

            contextMenu.getItems().add(deleteItem);

            // 告訴積木：當有人對你按右鍵 (ContextMenuRequested) 時，就把選單秀出來
            realBlock.setOnContextMenuRequested(e -> {
                contextMenu.show(realBlock, event.getScreenX(), event.getScreenY());
            });
        });
    }

    // updateCodeArea
    private void updateCodeArea() {
        // 1. 把畫布上所有積木抓出來
        java.util.List<javafx.scene.Node> blocks = new java.util.ArrayList<>();
        for (var node : workspace.getChildren()) {
            if (node instanceof DraggableBlock || node instanceof VariableBlock) {
                blocks.add(node);
            }
        }

        // 2. 關鍵：依照 Y 座標 (高低) 排序，上面的積木先印
        blocks.sort((a, b) -> Double.compare(a.getLayoutY(), b.getLayoutY()));

        // 3. 組合程式碼
        StringBuilder sb = new StringBuilder();
        for (var block : blocks) {
            if (block instanceof DraggableBlock) {
                sb.append(((DraggableBlock) block).getJavaCode()).append("\n");
            } else if (block instanceof VariableBlock) {
                sb.append(((VariableBlock) block).getJavaCode()).append("\n");
            }
        }
        codePreview.setText(sb.toString());
    }

    private void handleSnapping(javafx.scene.Node draggedBlock) {
        double SNAP_THRESHOLD = 30.0;
        double BLOCK_HEIGHT = 40.0; // 假設積木高度統一為 40

        // 1. 先用一樣的邏輯找出這塊積木的「尾巴」
        java.util.List<javafx.scene.Node> tail = new java.util.ArrayList<>();
        double currentY = draggedBlock.getLayoutY();
        boolean found = true;
        while (found) {
            found = false;
            for (javafx.scene.Node node : workspace.getChildren()) {
                if (node != draggedBlock && !tail.contains(node)) {
                    if (Math.abs(node.getLayoutX() - draggedBlock.getLayoutX()) < 5 &&
                            Math.abs(node.getLayoutY() - (currentY + BLOCK_HEIGHT)) < 5) {
                        tail.add(node);
                        currentY = node.getLayoutY();
                        found = true;
                        break;
                    }
                }
            }
        }

        // 2. 開始掃描畫布，尋找可以吸附的目標
        for (javafx.scene.Node target : workspace.getChildren()) {
            // 防呆：不可以跟自己吸附，也不可以跟自己底下的尾巴吸附！
            if (target == draggedBlock || tail.contains(target)) continue;

            double targetX = target.getLayoutX();
            double targetY = target.getLayoutY();

            // 情況 A：往下接 (自己的上方 靠近 別人的下方)
            double deltaX_below = Math.abs(draggedBlock.getLayoutX() - targetX);
            double deltaY_below = Math.abs(draggedBlock.getLayoutY() - (targetY + BLOCK_HEIGHT));

            // 情況 B：往上接 (自己的下方 靠近 別人的上方)
            double deltaX_above = Math.abs(draggedBlock.getLayoutX() - targetX);
            double deltaY_above = Math.abs((draggedBlock.getLayoutY() + BLOCK_HEIGHT) - targetY);

            double snapDeltaX = 0;
            double snapDeltaY = 0;

            // 判斷到底觸發了哪一種接合
            if (deltaX_below < SNAP_THRESHOLD && deltaY_below < SNAP_THRESHOLD) {
                snapDeltaX = targetX - draggedBlock.getLayoutX();
                snapDeltaY = (targetY + BLOCK_HEIGHT) - draggedBlock.getLayoutY();
            } else if (deltaX_above < SNAP_THRESHOLD && deltaY_above < SNAP_THRESHOLD) {
                snapDeltaX = targetX - draggedBlock.getLayoutX();
                snapDeltaY = (targetY - BLOCK_HEIGHT) - draggedBlock.getLayoutY();
            }

            // 如果成功觸發吸附
            if (snapDeltaX != 0 || snapDeltaY != 0) {
                // 強制移動主角的座標進行對齊
                draggedBlock.setLayoutX(draggedBlock.getLayoutX() + snapDeltaX);
                draggedBlock.setLayoutY(draggedBlock.getLayoutY() + snapDeltaY);

                // 底下的整串尾巴也要跟著被帶過去對齊！
                for (javafx.scene.Node t : tail) {
                    t.setLayoutX(t.getLayoutX() + snapDeltaX);
                    t.setLayoutY(t.getLayoutY() + snapDeltaY);
                }
                break; // 只要成功吸附到一個就停止掃描
            }
        }
    }
}