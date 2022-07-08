// SPDX-FileCopyrightText: 2020-present Open Networking Foundation <info@opennetworking.org>
//
// SPDX-License-Identifier: Apache-2.0

package tlc2.overrides;

/**
 * TLC overrides registry.
 */
public class TLCOverrides implements ITLCOverrides {
  @SuppressWarnings("rawtypes")
  @Override
  public Class[] get() {
    return new Class[]{JsonUtils.class, KafkaUtils.class};
  }
}
