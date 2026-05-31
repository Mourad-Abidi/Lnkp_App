package com.linkup.app.models;

public class VaultFileModel {
    public enum Category {
        DOCS, IMAGES, OTHER
    }

    private String fileName;
    private String fileSize;
    private String dateAdded;
    private int iconRes;
    private Category category;

    public VaultFileModel(String fileName, String fileSize, String dateAdded, int iconRes) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.dateAdded = dateAdded;
        this.iconRes = iconRes;
        this.category = determineCategory(fileName);
    }

    private Category determineCategory(String fileName) {
        String name = fileName.toLowerCase();
        if (name.endsWith(".pdf.enc") || name.endsWith(".txt.enc") || name.endsWith(".doc.enc")) {
            return Category.DOCS;
        } else if (name.endsWith(".jpg.enc") || name.endsWith(".png.enc") || name.endsWith(".jpeg.enc")) {
            return Category.IMAGES;
        } else {
            return Category.OTHER;
        }
    }

    public String getFileName() { return fileName; }
    public String getFileSize() { return fileSize; }
    public String getDateAdded() { return dateAdded; }
    public int getIconRes() { return iconRes; }
    public Category getCategory() { return category; }
}
