{ config, pkgs, modulesPath, ... }: {
  imports = [ "${modulesPath}/virtualisation/amazon-image.nix" ];
  ec2.hvm = true;
  
  environment.systemPackages = 
    [ pkgs.vim
      pkgs.htop
      pkgs.tmux
      pkgs.git
      pkgs.leiningen
      pkgs.openjdk11
      pkgs.mongodb 
      pkgs.mongodb-tools 
    ];

  services = {
    mongodb = {
      enable = true;
    };
  };

  security.acme.acceptTerms = true;
  security.acme.email = "pushpankark@gmail.com";
  services.nginx = {
    enable = true;
    recommendedGzipSettings = true;
    recommendedOptimisation = true;
    recommendedProxySettings = true;
    virtualHosts."launchitbeforeyoumakeit.com" = {
      enableACME = true;
      forceSSL = true;
      locations."/" = {
        proxyPass = "http://127.0.0.1:3000";
        proxyWebsockets = true; # needed if you need to use WebSocket
      };
    };
  };

  networking.firewall = {
    enable = true;
    allowedTCPPorts = [80 443 3000 8443];
  };
}
