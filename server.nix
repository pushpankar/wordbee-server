{ config, lib, pkgs, ... }:

{
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
      locations."/".root = "${pkgs.nginx}/html";
    };
  };
}
