package dev.franjaresc.wolfandfox.route.application.port.out;

import dev.franjaresc.wolfandfox.route.domain.UserId;

/** Who is performing the current action. It is the seam for real authentication later on. */
public interface CurrentUserPort {

    UserId currentUser();
}
