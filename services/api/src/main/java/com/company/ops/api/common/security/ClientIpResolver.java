package com.company.ops.api.common.security;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class ClientIpResolver {
  private static final Pattern IP_LITERAL = Pattern.compile("[0-9A-Fa-f:.]{2,45}");
  private final List<CidrBlock> trustedProxies;

  public ClientIpResolver(
      @Value("${ops.security.trusted-proxy-cidrs:127.0.0.0/8,::1/128}") String cidrs) {
    trustedProxies = Arrays.stream(cidrs.split(","))
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .map(CidrBlock::parse)
        .toList();
  }

  public String resolve(HttpServletRequest request) {
    String remote = normalize(request.getRemoteAddr());
    if (remote == null) return "unknown";
    if (!isTrusted(remote)) return remote;

    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null) {
      String[] chain = forwarded.split(",");
      String lastValid = null;
      for (int index = chain.length - 1; index >= 0; index--) {
        String candidate = normalize(chain[index]);
        if (candidate == null) continue;
        lastValid = candidate;
        if (!isTrusted(candidate)) return candidate;
      }
      if (lastValid != null) return lastValid;
    }

    String realIp = normalize(request.getHeader("X-Real-IP"));
    return realIp == null ? remote : realIp;
  }

  private boolean isTrusted(String address) {
    try {
      InetAddress parsed = InetAddress.getByName(address);
      return trustedProxies.stream().anyMatch(block -> block.contains(parsed));
    } catch (UnknownHostException exception) {
      return false;
    }
  }

  private String normalize(String value) {
    if (value == null) return null;
    String candidate = value.trim();
    if (!IP_LITERAL.matcher(candidate).matches()) return null;
    try {
      InetAddress.getByName(candidate);
      return candidate;
    } catch (UnknownHostException exception) {
      return null;
    }
  }

  private record CidrBlock(byte[] network, int prefixLength) {
    private static CidrBlock parse(String value) {
      String[] parts = value.split("/", -1);
      try {
        byte[] address = InetAddress.getByName(parts[0]).getAddress();
        int bits = address.length * 8;
        int prefix = parts.length == 1 ? bits : Integer.parseInt(parts[1]);
        if (parts.length > 2 || prefix < 0 || prefix > bits) throw new IllegalArgumentException();
        return new CidrBlock(address, prefix);
      } catch (UnknownHostException | NumberFormatException exception) {
        throw new IllegalArgumentException("Invalid trusted proxy CIDR: " + value, exception);
      }
    }

    private boolean contains(InetAddress address) {
      byte[] candidate = address.getAddress();
      if (candidate.length != network.length) return false;
      int fullBytes = prefixLength / 8;
      int remainingBits = prefixLength % 8;
      for (int index = 0; index < fullBytes; index++) {
        if (candidate[index] != network[index]) return false;
      }
      if (remainingBits == 0) return true;
      int mask = 0xff << (8 - remainingBits);
      return (candidate[fullBytes] & mask) == (network[fullBytes] & mask);
    }
  }
}
