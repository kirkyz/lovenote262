/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/lovenote262).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.waypoints.events;

import meteordevelopment.meteorclient.systems.waypoints.Waypoint;

public record WaypointRemovedEvent(Waypoint waypoint) {
}
