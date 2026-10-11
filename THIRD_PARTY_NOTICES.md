# Third-party notices

The MIT license applies to original dui sources and original procedural assets. Dependencies and game assets retain their respective licenses and terms.

- **Gson**: Apache License 2.0, https://github.com/google/gson. Used for JSON metadata; the demo bundles this dependency.
- **Gradle Wrapper**: Apache License 2.0, https://github.com/gradle/gradle. The checked-in wrapper bootstraps the pinned Gradle distribution.
- **Paper / Minestom / Adventure**: platform APIs supplied by the host; the DUI adapter JARs do not include their classes. See https://github.com/PaperMC/Paper, https://github.com/Minestom/Minestom and https://github.com/KyoriPowered/adventure.
- **LWJGL / shaderc**: optional build-time shader validation dependencies, not packaged in DUI runtime JARs. LWJGL is BSD-3-Clause (https://github.com/LWJGL/lwjgl3); shaderc is Apache-2.0 (https://github.com/google/shaderc).
- **Minecraft**: client JARs, font artwork and vanilla item definitions are external, versioned build inputs. They are not licensed under dui's MIT license and are not checked into these repositories. The generated local pack derives shifted fonts and item wrappers from those inputs; do not treat generated game-derived material as original MIT artwork.
