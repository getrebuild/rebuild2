/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author devezhao
 * @since 2020/07/29
 */
@SpringBootApplication
public class RebuildApplication {

	public static void main(String[] args) {
		SpringApplication.run(RebuildApplication.class, args);
	}

}
