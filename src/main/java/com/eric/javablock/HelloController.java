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
import java.util.ArrayList;
import java.util.List;

public class HelloController {

    @FXML private VBox paletteBox; // 左邊的工具箱
    @FXML private Pane workspace; // 中間的畫布
    @FXML private TextArea codePreview; // 右邊的程式碼預覽

    // 【新增】垃圾桶區域物件
    private StackPane trashZone;

    @FXML
    public void initialize() {
        // 1. 輸出/輸入類 (藍色)
        createPaletteItem("System.out.println", Color.DODGERBLUE, "System.out.println(\"Hello World\");");

        // 2. 變數宣告類 (橘色)
        createVariablePaletteItem();

        // 3. 條件判斷類 (【升級】改用容器積木召喚法，設定為金色)
        createControlPaletteItem("if");

        // 4. 迴圈控制類 (綠色)
        createControlPaletteItem("for");
        createControlPaletteItem("while");

        // 【新增】物件導向類 (紫色)
        createControlPaletteItem("class");
        createControlPaletteItem("method");
        // 【新增】初始化垃圾桶區域並固定在右下角
        createTrashZoneUI();
    }

    // --- 產生垃圾桶 UI 邏輯 ---
    private void createTrashZoneUI() {
        trashZone = new StackPane();
        Rectangle rect = new Rectangle(120, 80, Color.CRIMSON);
        rect.setArcWidth(15); rect.setArcHeight(15);
        rect.setStroke(Color.WHITE);
        rect.setStrokeWidth(2);

        Text text = new Text("🗑️ 拖曳至此刪除");
        text.setFill(Color.WHITE);
        text.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        trashZone.getChildren().addAll(rect, text);

        // 【關鍵修復】刪除原本的 .bind()，改用 AnchorPane 專屬的「錨點定位」
        // 這代表：把垃圾桶釘在距離底部 20px、距離右邊 20px 的位置
        javafx.scene.layout.AnchorPane.setBottomAnchor(trashZone, 20.0);
        javafx.scene.layout.AnchorPane.setRightAnchor(trashZone, 20.0);

        trashZone.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                // 將畫布上所有的「積木」拔除，但保留垃圾桶本身不受影響
                workspace.getChildren().removeIf(node ->
                        node instanceof DraggableBlock ||
                                node instanceof VariableBlock ||
                                node instanceof ControlBlock
                );
                // 同步清空右側程式碼
                updateCodeArea();
            }
        });

        workspace.getChildren().add(trashZone);
    }

    // --- 核心：檢查積木是否被丟進垃圾桶 ---
    private boolean handleTrashCheck(javafx.scene.Node draggedBlock) {
        // 先找出它的整串尾巴
        List<javafx.scene.Node> tail = getWorkspaceTail(draggedBlock);

        // 1. 預設為沒碰到，先檢查「頭」有沒有碰到垃圾桶
        boolean hitTrash = trashZone.getBoundsInParent().intersects(draggedBlock.getBoundsInParent());

        // 2. 如果頭沒碰到，就去檢查底下的「任何一塊尾巴」有沒有碰到
        if (!hitTrash) {
            for (javafx.scene.Node t : tail) {
                if (trashZone.getBoundsInParent().intersects(t.getBoundsInParent())) {
                    hitTrash = true;
                    break; // 只要有一塊碰到就算數，提早結束檢查
                }
            }
        }

        // 3. 如果確認有碰到垃圾桶，執行連坐行刑
        if (hitTrash) {
            // 刪除主角
            removeBlockFromParent(draggedBlock);

            // 連帶刪除整串肉粽尾巴
            for (javafx.scene.Node t : tail) {
                removeBlockFromParent(t);
            }
            return true; // 成功刪除
        }
        return false;
    }

    private void removeBlockFromParent(javafx.scene.Node node) {
        if (node.getParent() instanceof VBox && node.getParent() != workspace) {
            ((VBox) node.getParent()).getChildren().remove(node);
        } else {
            workspace.getChildren().remove(node);
        }
    }

    // --- 產生一般積木 ---
    private void createPaletteItem(String label, Color color, String code) {
        StackPane item = createPaletteUI(label, color);
        item.setOnMousePressed(event -> {
            DraggableBlock realBlock = new DraggableBlock(label, color, code);
            handlePaletteDrag(item, realBlock, 150);
        });
    }

    // --- 產生變數積木 ---
    private void createVariablePaletteItem() {
        StackPane item = createPaletteUI("Create Variable", Color.DARKORANGE);
        item.setOnMousePressed(event -> {
            VariableBlock realBlock = new VariableBlock();
            realBlock.setOnKeyReleased(k -> updateCodeArea());
            realBlock.setOnMouseClicked(m -> updateCodeArea());
            handlePaletteDrag(item, realBlock, 280);
        });
    }

    // --- 產生容器積木 (if, for, while) ---
    private void createControlPaletteItem(String type) {
        // 【修改】動態決定工具箱積木的顏色：class 用紫色，if 用金色，其餘用綠色
        Color color = Color.MEDIUMSEAGREEN;
        if (type.equals("class")) {
            color = Color.PURPLE;
        } else if (type.equals("method")) {
            color = Color.MEDIUMPURPLE;
        } else if (type.equals("if")) {
            color = Color.GOLD;
        }

        String labelText;
        if (type.equals("class")) {
            labelText = "class block";
        } else if (type.equals("method")) {
            labelText = "method block";
        } else {
            labelText = type + (type.equals("if") ? " block" : " loop");
        }

        StackPane item = createPaletteUI(labelText, color);
        item.setOnMousePressed(event -> {
            ControlBlock realBlock = new ControlBlock(type);
            realBlock.setOnKeyReleased(k -> updateCodeArea());

            // 【修改】根據不同容器寬度，調整拖拉時的滑鼠中心點
            double dragWidth = 200;
            if (type.equals("for")) {
                dragWidth = 350;
            } else if (type.equals("method")) {
                dragWidth = 320; // <--- 【修改】對齊 ControlBlock 裡設定的 320px
            }

            handlePaletteDrag(item, realBlock, dragWidth);
        });
    }

    // --- 左側工具箱UI小幫手 ---
    private StackPane createPaletteUI(String textLabel, Color color) {
        StackPane item = new StackPane();
        Rectangle rect = new Rectangle(150, 40, color);
        rect.setArcWidth(10); rect.setArcHeight(10);
        Text text = new Text(textLabel);
        text.setFill(color == Color.GOLD ? Color.BLACK : Color.WHITE);
        item.getChildren().addAll(rect, text);
        paletteBox.getChildren().add(item);
        return item;
    }

    // --- 統一的左側拉出邏輯 ---
    private void handlePaletteDrag(StackPane item, javafx.scene.Node realBlock, double width) {
        workspace.getChildren().add(realBlock);

        item.setOnMouseDragged(dragEvent -> {
            Point2D localPoint = workspace.sceneToLocal(dragEvent.getSceneX(), dragEvent.getSceneY());
            realBlock.setLayoutX(Math.max(0, localPoint.getX() - (width / 2.0)));
            realBlock.setLayoutY(Math.max(0, localPoint.getY() - 20));
        });

        item.setOnMouseReleased(e -> {
            item.setOnMouseDragged(null);
            // 【修復】放開時優先檢查垃圾桶，沒進垃圾桶才進行巢狀或吸附判定
            if (!handleTrashCheck(realBlock)) {
                if (!handleNesting(realBlock)) handleSnapping(realBlock);
            }
            updateCodeArea();
        });

        setupBlockEvents(realBlock);
    }

    // --- 為真積木綁定事件 ---
    private void setupBlockEvents(javafx.scene.Node realBlock) {
        // 從肚子裡拉出來的邏輯
        realBlock.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            javafx.scene.Parent parent = realBlock.getParent();
            if (parent instanceof VBox && parent != workspace && parent != paletteBox) {
                VBox belly = (VBox) parent;
                int index = belly.getChildren().indexOf(realBlock);
                if (index != -1) {
                    List<javafx.scene.Node> toExtract = new ArrayList<>(belly.getChildren().subList(index, belly.getChildren().size()));
                    for (javafx.scene.Node n : toExtract) {
                        Point2D global = n.localToScene(0, 0);
                        Point2D wsPos = workspace.sceneToLocal(global);
                        belly.getChildren().remove(n);
                        workspace.getChildren().add(n);
                        n.setLayoutX(wsPos.getX());
                        n.setLayoutY(wsPos.getY());
                        n.toFront();
                    }
                }
            }
        });

        realBlock.setOnMouseReleased(e -> {
            // 【修復】每次在畫布上重新拖拉放開時，也優先檢查有沒有丟進垃圾桶
            if (!handleTrashCheck(realBlock)) {
                if (!handleNesting(realBlock)) handleSnapping(realBlock);
            }
            updateCodeArea();
        });
    }

    // --- 巢狀吞噬判定 ---
    private boolean handleNesting(javafx.scene.Node draggedBlock) {
        for (javafx.scene.Node target : workspace.getChildren()) {
            if (target instanceof ControlBlock && target != draggedBlock) {
                ControlBlock cb = (ControlBlock) target;
                Point2D center = draggedBlock.localToScene(draggedBlock.getBoundsInLocal().getWidth() / 2, 10);
                Point2D localInCb = cb.innerContainer.sceneToLocal(center);

                if (cb.innerContainer.contains(localInCb)) {
                    List<javafx.scene.Node> tail = getWorkspaceTail(draggedBlock);

                    workspace.getChildren().remove(draggedBlock);
                    cb.innerContainer.getChildren().add(draggedBlock);
                    draggedBlock.setLayoutX(0); draggedBlock.setLayoutY(0);

                    for (javafx.scene.Node t : tail) {
                        workspace.getChildren().remove(t);
                        cb.innerContainer.getChildren().add(t);
                        t.setLayoutX(0); t.setLayoutY(0);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    // --- 吸附邏輯 ---
    private void handleSnapping(javafx.scene.Node draggedBlock) {
        List<javafx.scene.Node> tail = getWorkspaceTail(draggedBlock);
        double draggedHeight = draggedBlock.getBoundsInParent().getHeight();
        double SNAP_THRESHOLD = 30.0;

        for (javafx.scene.Node target : workspace.getChildren()) {
            if (target == draggedBlock || tail.contains(target)) continue;

            double targetX = target.getLayoutX();
            double targetY = target.getLayoutY();
            double targetHeight = target.getBoundsInParent().getHeight();

            double deltaX_below = Math.abs(draggedBlock.getLayoutX() - targetX);
            double deltaY_below = Math.abs(draggedBlock.getLayoutY() - (targetY + targetHeight));
            double deltaX_above = Math.abs(draggedBlock.getLayoutX() - targetX);
            double deltaY_above = Math.abs((draggedBlock.getLayoutY() + draggedHeight) - targetY);

            double snapDeltaX = 0, snapDeltaY = 0;

            if (deltaX_below < SNAP_THRESHOLD && deltaY_below < SNAP_THRESHOLD) {
                snapDeltaX = targetX - draggedBlock.getLayoutX();
                snapDeltaY = (targetY + targetHeight) - draggedBlock.getLayoutY();
            } else if (deltaX_above < SNAP_THRESHOLD && deltaY_above < SNAP_THRESHOLD) {
                snapDeltaX = targetX - draggedBlock.getLayoutX();
                snapDeltaY = (targetY - draggedHeight) - draggedBlock.getLayoutY();
            }

            if (snapDeltaX != 0 || snapDeltaY != 0) {
                draggedBlock.setLayoutX(draggedBlock.getLayoutX() + snapDeltaX);
                draggedBlock.setLayoutY(draggedBlock.getLayoutY() + snapDeltaY);
                for (javafx.scene.Node t : tail) {
                    t.setLayoutX(t.getLayoutX() + snapDeltaX);
                    t.setLayoutY(t.getLayoutY() + snapDeltaY);
                }
                break;
            }
        }
    }

    // --- 找尾巴小幫手 ---
    private List<javafx.scene.Node> getWorkspaceTail(javafx.scene.Node head) {
        List<javafx.scene.Node> tail = new ArrayList<>();
        double currentY = head.getLayoutY();
        double currentHeight = head.getBoundsInParent().getHeight();
        boolean found = true;
        while (found) {
            found = false;
            for (javafx.scene.Node node : workspace.getChildren()) {
                if (node != head && !tail.contains(node)) {
                    if (Math.abs(node.getLayoutX() - head.getLayoutX()) < 5 &&
                            Math.abs(node.getLayoutY() - (currentY + currentHeight)) < 5) {
                        tail.add(node);
                        currentY = node.getLayoutY();
                        currentHeight = node.getBoundsInParent().getHeight();
                        found = true;
                        break;
                    }
                }
            }
        }
        return tail;
    }

    // --- 更新程式碼 ---
    private void updateCodeArea() {
        List<javafx.scene.Node> blocks = new ArrayList<>();
        for (var node : workspace.getChildren()) {
            if (node instanceof DraggableBlock || node instanceof VariableBlock || node instanceof ControlBlock) {
                blocks.add(node);
            }
        }
        blocks.sort((a, b) -> Double.compare(a.getLayoutY(), b.getLayoutY()));

        StringBuilder sb = new StringBuilder();
        for (var block : blocks) {
            if (block instanceof DraggableBlock) sb.append(((DraggableBlock) block).getJavaCode()).append("\n");
            else if (block instanceof VariableBlock) sb.append(((VariableBlock) block).getJavaCode()).append("\n");
            else if (block instanceof ControlBlock) sb.append(((ControlBlock) block).getJavaCode()).append("\n");
        }
        codePreview.setText(sb.toString());
    }
}