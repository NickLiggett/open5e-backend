package com.main.app.ownership;

import com.main.app.user.CurrentUser;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Supplies the {@code userId} parameter of the visibility filter. Hibernate gets this bean from Spring each time the
 * filter is applied.
 */
@Component
public class VisibilityUserIdResolver implements Supplier<Long> {

    private final CurrentUser currentUser;

    public VisibilityUserIdResolver(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @Override
    public Long get() {
        return currentUser.id().orElse(Visibility.ANONYMOUS);
    }
}
