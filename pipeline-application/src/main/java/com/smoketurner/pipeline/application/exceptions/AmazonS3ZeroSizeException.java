/**
 * Copyright 2018 Smoke Turner, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smoketurner.pipeline.application.exceptions;

public class AmazonS3ZeroSizeException extends Exception {

    private static final long serialVersionUID = 1L;

    public AmazonS3ZeroSizeException() {
        super();
    }

    public AmazonS3ZeroSizeException(final Throwable cause) {
        super(cause);
    }

    public AmazonS3ZeroSizeException(final String message) {
        super(message);
    }

    public AmazonS3ZeroSizeException(final String message,
            final Throwable cause) {
        super(message, cause);
    }
}
