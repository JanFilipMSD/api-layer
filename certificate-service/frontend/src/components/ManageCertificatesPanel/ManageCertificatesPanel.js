import React, {useMemo, useState} from "react";
import Styles from "./Style";
import {useField, useForm} from "react-final-form-hooks";
import {Box, FormControlLabel, Switch} from "@material-ui/core";
import {ListItem, TextareaAutosize, Typography} from "@mui/material";
import List from '@mui/material/List';
import ListItemText from '@mui/material/ListItemText';
import https from'https';

const ManageCertificatesPanel = () => {
    const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
    const [pemCert, setPemCert] = React.useState(null);
    const [serviceUrl, setServiceUrl] = React.useState(null);
    const [certAlias, setCertAlias] = useState(null);
    const [errors, setErrors] = React.useState(null);
    const [loading, setLoading] = React.useState(false);
    const [trustedCerts, setTrustedCerts] = React.useState(null);

    const onSubmit = async values => {
        await sleep(300);
        window.alert(JSON.stringify(values, 0, 2));

        fetch("/certificate/upload", {
            method: 'POST',
            mode: 'cors',
            body: JSON.stringify({
                url: alias.input.value,
                certificate: certificate.input.value
            })
        });
    };

    const validate = values => {
        const errors = {};
        setCertAlias(values.alias);
        setServiceUrl(values.url);

        if (!values.url) {
            errors.url = "Required";
        }
        if (!values.alias) {
            errors.alias = "Required";
        }
        return errors;
    };

    const initialValues = useMemo(
        () => ({
            alias: "",
            url: "",
            certificate: ""
        }),
        []
    );

    const httpsAgent = new https.Agent({
        rejectUnauthorized: false,
    });

    // TODO get list of certificate
    const getListOfTrustedCertificates = async () => {
        const url = process.env.REACT_APP_GATEWAY_URL + `/certificate-service/api/v1/trusted-certs`
        fetch(url, {
            method: 'GET',
            mode: 'cors',
            agent: httpsAgent,
        }).then((response) => {
            if (!response.ok) {
                throw Error(response.statusText);
            }
            return response.text();
        }).then(result => {
            setTrustedCerts(JSON.parse(result));
        }).catch((error) => {
            console.log(error)
            setErrors(error.message);
        });
    }
    const getCertificateInPemFormat = async () => {
        const url = process.env.REACT_APP_GATEWAY_URL + `/certificate-service/api/v1/certificate?url=${serviceUrl}`
        fetch(url, {
            method: 'GET',
            mode: 'cors'
        }).then((response) => {
            if (!response.ok) {
                throw Error(response.statusText);
            }
            return response.text();
        }).then(result => {
            setPemCert(result);
        }).catch((error) => {
            console.log(error)
            setErrors(error.message);
        });
    }

    const {form} = useForm({
        onSubmit,
        initialValues,
        validate
    });

    const alias = useField("alias", form);
    const url = useField("url", form);
    const certificate = useField("certificate", form);

    const addCertificate = () => {
        console.log("Add Certificate");
    }

    const items = [];
    if (trustedCerts !== null) {
        for (const item in trustedCerts) {
            items.push(
                <List>
                <ListItem alignItems={"flex-start"}>
                    <ListItemText primary={`LABEL: ${item}`}
                                  secondary={<React.Fragment>
                                      <Typography
                                          sx={{ display: 'inline' }}
                                          component="span"
                                          variant="body2"
                                          color="text.primary"
                                      >
                                          Distinguished Name:
                                      </Typography>
                                      {`${trustedCerts[item]}`}
                                  </React.Fragment>}
                                  />
                </ListItem>
                </List>
        )

        }

    }

    return (
        <Styles>
            <h2>Manage truststore</h2>

            <form>
                <Box sx={{'& > button': {m: 1}}}>
                    <FormControlLabel
                        sx={{
                            display: 'block',
                        }}
                        control={
                            <Switch
                                checked={loading}
                                onChange={() => setLoading(!loading)}
                                name="loading"
                                color="primary"
                            />
                        }
                        label="Retrieve certificate"
                    />
                </Box>
                <div>
                    <label disabled={!loading}>Hostname</label>
                    <input disabled={!loading} {...url.input} placeholder="Service Hostname"/>
                    {url.meta.touched && url.meta.error && (
                        <span>{url.meta.error}</span>
                    )}
                </div>
                <div className="buttons">
                    <button disabled={!loading} type="submit" onClick={getCertificateInPemFormat}>
                        Get certificate
                    </button>
                </div>
                {pemCert !== null && (
                    <TextareaAutosize
                        aria-label="minimum height"
                        minRows={3}
                        defaultValue={pemCert}
                        style={{width: 200}}
                    />
                )}
                <div className="buttons">
                    <button type="submit" onClick={getListOfTrustedCertificates}>
                        Get list of trusted certificates
                    </button>
                </div>
                {trustedCerts !== null && (
                    <List sx={{width: '100%', maxWidth: 360, bgcolor: 'background.paper'}}>
                        {items}
                    </List>
                )}

                <div>
                    <label>Alias</label>
                    <input {...alias.input} placeholder="Certificate Alias"/>
                    {alias.meta.touched && alias.meta.error && (
                        <span>{alias.meta.error}</span>
                    )}
                </div>
                <div>
                    <label>Certificate to use</label>
                    <input {...certificate.input} type="textarea" placeholder="Provide base64 encoded certificate."/>
                    {certificate.meta.touched && certificate.meta.error && (
                        <span>{certificate.meta.error}</span>
                    )}
                </div>

                <div className="buttons">
                    <button type="submit" onClick={() => addCertificate()}>
                        Add
                    </button>
                </div>
            </form>
        </Styles>
    );

}


export default ManageCertificatesPanel;


