/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.docker.compose.connection;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PortsShould {

    public static final String LOCALHOST_IP = "127.0.0.1";
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final DockerPort port = mock(DockerPort.class);

    @Before
    public void setup() {
        when(port.getInternalPort()).thenReturn(7001);
        when(port.getExternalPort()).thenReturn(7000);
    }

    @Test
    public void result_in_no_ports_when_there_are_no_ports_in_ps_output() throws IOException, InterruptedException {
        String psOutput = "------";
        Ports ports = Ports.parseFromDockerComposePs(psOutput, null);
        Ports expected = new Ports(emptyList());
        assertThat(ports, is(expected));
    }

    @Test
    public void result_in_single_port_when_there_is_single_tcp_port_mapping() throws IOException, InterruptedException {
        String psOutput = "0.0.0.0:5432->5432/tcp";
        Ports ports = Ports.parseFromDockerComposePs(psOutput, LOCALHOST_IP);
        Ports expected = new Ports(newArrayList(new DockerPort(LOCALHOST_IP, 5432, 5432)));
        assertThat(ports, is(expected));
    }

    @Test
    public void
            result_in_single_port_with_ip_other_than_localhost_when_there_is_single_tcp_port_mapping()
            throws IOException, InterruptedException {
        String psOutput = "10.0.1.2:1234->2345/tcp";
        Ports ports = Ports.parseFromDockerComposePs(psOutput, LOCALHOST_IP);
        Ports expected = new Ports(newArrayList(new DockerPort("10.0.1.2", 1234, 2345)));
        assertThat(ports, is(expected));
    }

    @Test
    public void result_in_two_ports_when_there_are_two_tcp_port_mappings() throws IOException, InterruptedException {
        String psOutput = "0.0.0.0:5432->5432/tcp, 0.0.0.0:5433->5432/tcp";
        Ports ports = Ports.parseFromDockerComposePs(psOutput, LOCALHOST_IP);
        Ports expected = new Ports(newArrayList(new DockerPort(LOCALHOST_IP, 5432, 5432),
                                                new DockerPort(LOCALHOST_IP, 5433, 5432)));
        assertThat(ports, is(expected));
    }

    @Test
    public void result_in_no_ports_when_there_is_a_non_mapped_exposed_port() throws IOException, InterruptedException {
        String psOutput = "5432/tcp";
        Ports ports = Ports.parseFromDockerComposePs(psOutput, LOCALHOST_IP);
        Ports expected = new Ports(emptyList());
        assertThat(ports, is(expected));
    }

    @Test
    public void parse_actual_docker_compose_output() throws IOException, InterruptedException {
        String psOutput =
                  "       Name                      Command               State                                         Ports                                        \n"
                + "-------------------------------------------------------------------------------------------------------------------------------------------------\n"
                + "magritte_magritte_1   /bin/sh -c /usr/local/bin/ ...   Up      0.0.0.0:7000->7000/tcp, 7001/tcp, 7002/tcp, 7003/tcp, 7004/tcp, 7005/tcp, 7006/tcp \n"
                + "";
        Ports ports = Ports.parseFromDockerComposePs(psOutput, LOCALHOST_IP);
        Ports expected = new Ports(newArrayList(new DockerPort(LOCALHOST_IP, 7000, 7000)));
        assertThat(ports, is(expected));
    }

    @Test
    public void throw_illegal_state_exception_when_no_running_container_found_for_service()
            throws IOException, InterruptedException {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("No container found");
        Ports.parseFromDockerComposePs("", "");
    }
}
