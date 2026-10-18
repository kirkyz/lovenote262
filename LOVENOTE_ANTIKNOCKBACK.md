# Anti-knockback implementation

- `com.lovenote.module.impl.Velocity` is now an activatable Lovenote module rather than an inert `UnportedModule`.
- Horizontal and vertical settings each range from 0% (no incoming attack knockback) to 100% (normal incoming knockback).
- The existing `LovenoteClientPlayNetworkHandlerMixin` uses the already-registered packet accessor to scale the local player's `ClientboundSetEntityMotionPacket` movement impulse, without cancelling the packet or scheduling a second motion application.
- When Meteor's own Velocity module is active, Lovenote defers to it to avoid double modification.
- Explosions, fluids, fishing rods, and entity pushing are **not** handled by the Lovenote module. Meteor's Velocity module has separate options for those.
- This is a client-side modification. Server-side movement checks can correct motion; no server compatibility is guaranteed.

## Verification

Source-level checks completed for the accessor, existing mixin registration, module registration, and absence of duplicate Lovenote Velocity registration. A full Gradle/Java 25 build and in-game test have not been completed in this environment.
