package co.edu.unicauca.domain;

import java.util.List;

/**
 * Resultado de una consulta paginada. Las páginas empiezan en 1.
 */
public class PageResult<T> {
    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final int totalItems;

    public PageResult(List<T> items, int page, int pageSize, int totalItems) {
        this.items = items;
        this.page = page;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
    }

    public List<T> getItems() { return items; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public int getTotalItems() { return totalItems; }

    public int getTotalPages() {
        return Math.max(1, (int) Math.ceil(totalItems / (double) pageSize));
    }

    public boolean hasPrevious() {
        return page > 1;
    }

    public boolean hasNext() {
        return page < getTotalPages();
    }
}
