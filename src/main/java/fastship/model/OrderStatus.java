package fastship.model;

/**
 * Order lifecycle statuses (Figure 5 - Class Diagram & Table 27).
 */
public enum OrderStatus {
    NEW,
    CONFIRMED,
    ASSIGNED,
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    FAILED,
    CANCELLED
}

