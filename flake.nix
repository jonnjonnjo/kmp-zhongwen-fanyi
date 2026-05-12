{
  description = "jzw — Chinese-learning terminal aid (translation + pinyin + per-token meanings)";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs = { self, nixpkgs }:
    let
      systems = [ "x86_64-linux" "aarch64-linux" "x86_64-darwin" "aarch64-darwin" ];
      forAllSystems = f:
        nixpkgs.lib.genAttrs systems (system: f (import nixpkgs { inherit system; }));
    in {
      packages = forAllSystems (pkgs: {
        default = pkgs.stdenv.mkDerivation (finalAttrs: {
          pname = "jzw";
          version = "0.1.0";
          src = ./.;

          nativeBuildInputs = [
            pkgs.gradle
            pkgs.makeWrapper
          ];

          mitmCache = pkgs.gradle.fetchDeps {
            inherit (finalAttrs) pname;
            data = ./nix/deps.json;
          };

          gradleBuildTask = ":tui:installDist";
          gradleFlags = [ "--no-daemon" ];

          installPhase = ''
            runHook preInstall
            mkdir -p $out
            cp -r tui/build/install/jzw/* $out/
            wrapProgram $out/bin/jzw \
              --set JAVA_HOME ${pkgs.jdk21.home}
            runHook postInstall
          '';

          meta = {
            description = "Chinese-learning aid: translation, pinyin, per-token meanings";
            homepage = "https://github.com/jonnjonnjo/kmp-zhongwen-fanyi";
            license = pkgs.lib.licenses.gpl3Only;
            mainProgram = "jzw";
            platforms = pkgs.lib.platforms.unix;
          };
        });
      });

      apps = forAllSystems (pkgs: {
        default = {
          type = "app";
          program = "${self.packages.${pkgs.system}.default}/bin/jzw";
        };
      });

      devShells = forAllSystems (pkgs: {
        default = pkgs.mkShell {
          packages = [ pkgs.jdk21 pkgs.gradle ];
        };
      });
    };
}
