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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar.ButtonData;
import java.util.ArrayList;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonArray;
import jakarta.json.JsonReader;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;



public class HelloController {

    @FXML private VBox paletteBox; // 左邊的工具箱
    @FXML private Pane workspace; // 中間的畫布
    @FXML private TextArea codePreview; // 右邊的程式碼預覽

    // 【新增】用來記錄當前專案的檔案指引（如果為 null 代表是全新未存檔的專案）
    private File currentProjectFile = null;
    private File defaultDirectory = new File(System.getProperty("user.home"), "Desktop");

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

        // 【新增】監聽 Ctrl+S 快捷鍵
        // 必須利用 runLater 確保 workspace 已經被渲染並加到 Scene 畫面中
        javafx.application.Platform.runLater(() -> {
            if (workspace.getScene() != null) {
                javafx.scene.input.KeyCombination ctrlS = new javafx.scene.input.KeyCodeCombination(
                        javafx.scene.input.KeyCode.S, javafx.scene.input.KeyCombination.CONTROL_DOWN);

                workspace.getScene().addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                    if (ctrlS.match(event)) {
                        onSaveProject(); // 觸發儲存邏輯
                        event.consume();
                    }
                });
            }
        });
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

    // 輔助方法：將畫布積木物件轉為 JSON 資料
    private JsonObject convertNodeToJson(javafx.scene.Node node) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("x", node.getLayoutX());
        builder.add("y", node.getLayoutY());

        if (node instanceof DraggableBlock) {
            DraggableBlock b = (DraggableBlock) node;
            builder.add("classType", "DraggableBlock");
            builder.add("blockType", b.getBlockType());
            builder.add("code", b.getJavaCode());
            return builder.build();
        }
        else if (node instanceof VariableBlock) {
            VariableBlock b = (VariableBlock) node;
            builder.add("classType", "VariableBlock");
            builder.add("blockType", b.getBlockType()); // 會得到 "variable"
            builder.add("varType", b.getSelectedType());
            builder.add("varName", b.getVarName());
            builder.add("varValue", b.getVarValue());
            return builder.build();
        }
        else if (node instanceof ControlBlock) {
            ControlBlock b = (ControlBlock) node;
            builder.add("classType", "ControlBlock");
            builder.add("blockType", b.getBlockType()); // 會得到 "if", "for", "while"

            // 遞迴打包內部嵌套的子積木
            JsonArrayBuilder innerArrayBuilder = Json.createArrayBuilder();
            for (var child : b.innerContainer.getChildren()) {
                JsonObject childJson = convertNodeToJson(child);
                if (childJson != null) innerArrayBuilder.add(childJson);
            }
            builder.add("innerBlocks", innerArrayBuilder.build());
            return builder.build();
        }
        return null;
    }

    // 輔助方法：將 JSON 資料還原成對應的 JavaFX UI 積木物件
    private javafx.scene.Node convertJsonToNode(JsonObject obj) {
        String classType = obj.getString("classType");
        double x = obj.getJsonNumber("x").doubleValue();
        double y = obj.getJsonNumber("y").doubleValue();

        if ("DraggableBlock".equals(classType)) {
            String blockType = obj.getString("blockType");
            String code = obj.getString("code");
            // 這裡暫時用原本工具箱預設的藍色還原
            DraggableBlock b = new DraggableBlock(blockType, Color.DODGERBLUE, code);
            b.setLayoutX(x);
            b.setLayoutY(y);

            // 【關鍵修復】讀檔時必須幫積木重新綁定事件，否則會卡死無法與其他積木分離！
            setupBlockEvents(b);
            return b;
        }
        else if ("VariableBlock".equals(classType)) {
            VariableBlock b = new VariableBlock();
            b.setLayoutX(x);
            b.setLayoutY(y);

            // 把 JSON 資料倒回去輸入框裡
            b.setVariableData(obj.getString("varType"), obj.getString("varName"), obj.getString("varValue"));

            // 核心：【重新綁定滑鼠拖曳監聽】讓讀出來的控制積木可以被自由拖動
            b.setOnKeyReleased(k -> updateCodeArea());
            b.setOnMouseClicked(m -> updateCodeArea());
            // 【關鍵修復】讀檔時必須幫積木重新綁定事件，否則會卡死無法與其他積木分離！
            setupBlockEvents(b);
            return b;
        }
        else if ("ControlBlock".equals(classType)) {
            String blockType = obj.getString("blockType");
            ControlBlock b = new ControlBlock(blockType);
            b.setLayoutX(x);
            b.setLayoutY(y);

            // 如果當初內部有塞其他小積木，遞迴還原出來並加進去容器裡
            if (obj.containsKey("innerBlocks")) {
                JsonArray innerArray = obj.getJsonArray("innerBlocks");
                for (int i = 0; i < innerArray.size(); i++) {
                    javafx.scene.Node childNode = convertJsonToNode(innerArray.getJsonObject(i));
                    if (childNode != null) {
                        b.innerContainer.getChildren().add(childNode);
                    }
                }
            }

            // 核心：【重新綁定滑鼠拖曳監聽】讓讀出來的控制積木可以被自由拖動
            b.setOnKeyReleased(k -> updateCodeArea());
            // 【關鍵修復】讀檔時必須幫積木重新綁定事件，否則會卡死無法與其他積木分離！
            setupBlockEvents(b);
            return b;
        }
        return null;
    }

    // 共用輔助方法 A：安全地套用當前設定的變數目錄到視窗中
    private void applyDefaultDirectory(FileChooser fileChooser) {
        if (defaultDirectory != null && defaultDirectory.exists() && defaultDirectory.isDirectory()) {
            fileChooser.setInitialDirectory(defaultDirectory);
        }
    }

    // 共用輔助方法 B：強行驅動系統檔案總管打開資料夾
    private void openDirectoryInExplorer(File dir) {
        try {
            if (dir.exists() && dir.isDirectory()) {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(dir);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // === 點擊按鈕或按下 Ctrl+S 都會執行的儲存功能 （自動判斷 覆蓋 或是 另存新檔） ===
    @FXML
    private void onSaveProject() {
        // 1. 判斷是否有現成檔案
        if (currentProjectFile != null) {
            // 有現成檔案，直接呼叫下方的「強行覆蓋寫入」方法，不再彈出視窗
            executeSave(currentProjectFile);
        } else {
            // 沒有現成檔案，開啟視窗引導使用者選擇位置
            showSaveAsDialog();
        }
    }

    // === 另存新檔 ===
    @FXML
    private void onSaveAsProject() {
        showSaveAsDialog();
    }

    // === 另存新檔 / 第一次儲存時調用的視窗 ===
    private void showSaveAsDialog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("另存新檔");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));

        // ⭐ 關鍵：另存新檔時，也強制指定為變更後的目錄
        if (defaultDirectory != null && defaultDirectory.exists() && defaultDirectory.isDirectory()) {
            fileChooser.setInitialDirectory(defaultDirectory);
        } else {
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home"), "Desktop"));
        }

        File file = fileChooser.showSaveDialog(paletteBox.getScene().getWindow());
        if (file != null) {
            // 使用者選好位置了，記錄下來，下次 Ctrl+S 就會直接覆蓋這個檔案
            defaultDirectory = file.getParentFile(); // 記憶最後存檔的位置
            javafx.stage.Stage stage = (javafx.stage.Stage) paletteBox.getScene().getWindow();

            if (stage != null) {
                // 將標題改為：專案名稱 - JavaBlock
                stage.setTitle(file.getName() + " - JavaBlock");
            }
            currentProjectFile = file;
            executeSave(file);
        }
    }

    // === 真正把畫布資料寫進實體檔案的核心方法 ===
    private void executeSave(File file) {
        JsonArrayBuilder rootArrayBuilder = Json.createArrayBuilder();

        // 遍歷畫布上所有的第一層積木
        for (var node : workspace.getChildren()) {
            JsonObject blockJson = convertNodeToJson(node);
            if (blockJson != null) {
                rootArrayBuilder.add(blockJson);
            }
        }

        // 設定 Pretty Print 排版
        Map<String, Boolean> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory writerFactory = Json.createWriterFactory(config);

        try (FileWriter fileWriter = new FileWriter(file);
             JsonWriter jsonWriter = writerFactory.createWriter(fileWriter)) {

            jsonWriter.writeArray(rootArrayBuilder.build());
            System.out.println("💾 檔案儲存成功：" + file.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("❌ 儲存失敗！");
            e.printStackTrace();
        }
    }

    // === 點擊按鈕執行的讀取功能 ===
    @FXML
    private void onLoadProject() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("開啟 JavaBlock 專案");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));

        // 關鍵：開啟舊檔時，強制指定為剛剛設定的變更目錄
        if (defaultDirectory != null && defaultDirectory.exists() && defaultDirectory.isDirectory()) {
            fileChooser.setInitialDirectory(defaultDirectory);
        } else {
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home"), "Desktop"));
        }

        File file = fileChooser.showOpenDialog(paletteBox.getScene().getWindow());
        if (file != null) {
            try (FileReader fileReader = new FileReader(file);
                 JsonReader jsonReader = Json.createReader(fileReader)) {

                JsonArray jsonArray = jsonReader.readArray();

                // ⭕ 【修正問題 2：垃圾桶不見了】
                // 不要用 workspace.getChildren().clear();
                // 改成唯獨只刪除積木元件，保留垃圾桶（假設垃圾桶不是這三種 Block）
                workspace.getChildren().removeIf(node ->
                        node instanceof DraggableBlock ||
                                node instanceof ControlBlock ||
                                node instanceof VariableBlock
                );

                // 讀取 JSON 陣列並還原積木
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject obj = jsonArray.getJsonObject(i);
                    javafx.scene.Node bNode = convertJsonToNode(obj);
                    if (bNode != null) {
                        workspace.getChildren().add(bNode);

                        // 【關鍵修正：滑鼠跑掉與積木卡死】
                        // 讀取出來後，為了讓它們的輸入框在打字時可以即時更新右邊的 Code
                        // 我們只綁定 KeyReleased 監聽，絕對不去用 setOnMousePressed 覆蓋積木自帶的拖拉事件！
                        if (bNode instanceof ControlBlock) {
                            ((ControlBlock) bNode).setOnKeyReleased(k -> updateCodeArea());
                        } else if (bNode instanceof VariableBlock) {
                            ((VariableBlock) bNode).setOnKeyReleased(k -> updateCodeArea());
                            ((VariableBlock) bNode).setOnMouseClicked(m -> updateCodeArea());
                        }
                    }
                }

                javafx.stage.Stage stage = (javafx.stage.Stage) paletteBox.getScene().getWindow();
                if (stage != null) {
                    stage.setTitle(file.getName() + " - JavaBlock");
                }

                // 還原後自動重新整理右側的 Java 程式碼預覽面板
                updateCodeArea();
                System.out.println("📂 專案已成功讀取！");
                defaultDirectory = file.getParentFile();
            } catch (Exception e) {
                System.err.println("讀取失敗！原因可能是檔案損毀或欄位不對。");
                e.printStackTrace();
            }
        }
    }

    // === 目錄管理核心 ： 彈出對話框問使用者：要「直接開啟目前目錄」還是「重新設定新目錄」？ ===
    @FXML
    private void onManageDefaultDirectory() {
        // 彈出一個選單讓使用者選擇要做什麼
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("預設目錄管理");
        alert.setHeaderText("目前預設目錄為：\n" + defaultDirectory.getAbsolutePath());
        alert.setContentText("請選擇您要執行的操作：");

        // 定義三個按鈕
        ButtonType btnOpen = new ButtonType("📂 直接開啟");
        ButtonType btnChange = new ButtonType("⚙️ 更改路徑");
        ButtonType btnCancel = new ButtonType("取消", ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnOpen, btnChange, btnCancel);

        // 根據使用者點擊的按鈕執行對應動作
        alert.showAndWait().ifPresent(response -> {
            if (response == btnOpen) {
                // 直接打開現有的預設目錄
                openDirectoryInExplorer(defaultDirectory);
            } else if (response == btnChange) {
                // 彈出資料夾選擇器，讓使用者挑選新目錄
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("設定預設工作目錄");

                // 如果當前目錄有效，就當作預設開啟位置
                if (defaultDirectory != null && defaultDirectory.exists()) {
                    directoryChooser.setInitialDirectory(defaultDirectory);
                }

                File selectedDirectory = directoryChooser.showDialog(paletteBox.getScene().getWindow());
                if (selectedDirectory != null) {
                    defaultDirectory = selectedDirectory; // 👈 確實更新全域變數
                    System.out.println("預設目錄已變更為: " + defaultDirectory.getAbsolutePath());
                }
            }
        });
    }

}