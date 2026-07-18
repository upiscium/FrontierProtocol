{
  description = "Minecraft 1.21.1 NeoForge mod development environment";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs = { nixpkgs, ... }:
    let
      systems = [ "x86_64-linux" "aarch64-linux" ];
      forAllSystems = nixpkgs.lib.genAttrs systems;
    in
    {
      devShells = forAllSystems (system:
        let
          pkgs = import nixpkgs { inherit system; };
          runtimeLibraries = with pkgs; [
            alsa-lib
            libGL
            libglvnd
            libpulseaudio
            libxkbcommon
            openal
            udev
            wayland
            libx11
            libxcursor
            libxext
            libxi
            libxrandr
            libxrender
            libxxf86vm
          ];
        in
        {
          default = pkgs.mkShell {
            packages = with pkgs; [
              git
              jdk21
            ];

            JAVA_HOME = "${pkgs.jdk21}";

            shellHook = ''
              export LD_LIBRARY_PATH="${pkgs.lib.makeLibraryPath runtimeLibraries}''${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
              export _JAVA_AWT_WM_NONREPARENTING=1
            '';
          };
        });
    };
}
