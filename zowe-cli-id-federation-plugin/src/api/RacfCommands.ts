/**
 * This program and the accompanying materials are made available and may be used, at your option, under either:
 * * Eclipse Public License v2.0, available at https://www.eclipse.org/legal/epl-v20.html, OR
 * * Apache License, version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import {ImperativeError, TextUtils} from "@zowe/imperative";
import { warn } from "console";
import * as fs from "fs";
import {IIdentity} from "./CsvParser";
import {hasValidLength} from "./ValidateUtil";
import {Constants} from "./Constants";
import {IHandlerResponseApi} from "@zowe/imperative/lib/cmd/src/doc/response/api/handler/IHandlerResponseApi";

export class RacfCommands {

    readonly MAX_LENGTH_MAINFRAME_ID = 8;
    readonly MAX_LENGTH_DISTRIBUTED_ID = 246;
    readonly MAX_LENGTH_LABEL = 32;

    constructor(
        private registry: string,
        private identities: IIdentity[],
        private response: IHandlerResponseApi
    ) {
    }

    getCommands(): string[] {
        const racfTemplate = fs.readFileSync('src/api/templates/racf.jcl').toString();
        const racfRefreshCommand = fs.readFileSync('src/api/templates/racf_refresh.jcl').toString();

        const racfCommands = this.identities
            .map(identity => this.getCommand(identity, racfTemplate))
            .filter(command => command);

        if (!racfCommands.some(Boolean)) {
            this.response.data.setExitCode(Constants.FATAL_CODE);
            throw new ImperativeError({msg: "Error when trying to create the identity mapping."});
        }
        racfCommands.push(racfRefreshCommand);
        return racfCommands;
    }

    private getCommand(identity: IIdentity, racfTemplate: string): string {
        if(!hasValidLength(identity.mainframeId, this.MAX_LENGTH_MAINFRAME_ID)) {
            warn(`The mainframe user ID '${identity.mainframeId}' has exceeded maximum length of ${this.MAX_LENGTH_MAINFRAME_ID} characters. ` +
           `Identity mapping for the user '${identity.userName}' has not been created.`);
            this.response.data.setExitCode(Constants.WARN_CODE);
            return '';
        }

        if(!hasValidLength(identity.distributedId, this.MAX_LENGTH_DISTRIBUTED_ID)) {
            warn(`The distributed user ID '${identity.distributedId}' has exceeded maximum length of ${this.MAX_LENGTH_DISTRIBUTED_ID} characters. ` +
                `Identity mapping for the user '${identity.userName}' has not been created.`);
            this.response.data.setExitCode(Constants.WARN_CODE);
            return '';
        }

        if(!hasValidLength(identity.userName, this.MAX_LENGTH_LABEL)) {
            warn(`The user name '${identity.userName}' has exceeded maximum length of ${this.MAX_LENGTH_LABEL} characters. ` +
                `Identity mapping for the user '${identity.userName}' has not been created.`);
            this.response.data.setExitCode(Constants.WARN_CODE);
            return '';
        }

        return TextUtils.renderWithMustache(racfTemplate, {
            mainframe_id: identity.mainframeId.trim(),
            distributed_id: identity.distributedId.trim(),
            registry: this.registry,
            user_name: identity.userName.trim(),
            escape: function() {
                return function(text: string, render: any) {
                    return render(text).replace(/'/g, "''");
                };
            }
        });
    }

}