import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import { Button } from 'react-bootstrap';
import { BootstrapModalForm, Input } from 'components/bootstrap';
import { IfPermitted } from 'components/common';
import ObjectUtils from 'util/ObjectUtils';

const IcingaPluginConfig = createReactClass({
    displayName: 'IcingaPluginConfig',

    propTypes: {
        config: PropTypes.object,
        updateConfig: PropTypes.func.isRequired,
    },

    getDefaultProps() {
        return {
            config: {
                icinga_endpoints: [],
                icinga_user: '',
                icinga_passwd: '',
                verify_ssl: true,
                ssl_ca_pem: '',
            },
        };
    },

    getInitialState() {
        return {
            config: ObjectUtils.clone(this.props.config),
        };
    },

    componentWillReceiveProps(newProps) {
        this.setState({ config: ObjectUtils.clone(newProps.config) });
    },

    _updateConfigField(field, value) {
        const update = ObjectUtils.clone(this.state.config);
        update[field] = value;
        this.setState({ config: update });
    },

    _onCheckboxClick(field, ref) {
        return () => {
            this._updateConfigField(field, this.refs[ref].getChecked());
        };
    },

    _onSelect(field) {
        return (selection) => {
            this._updateConfigField(field, selection);
        };
    },

    _onUpdate(field) {
        return e => {
            this._updateConfigField(field, e.target.value);
        };
    },

    _openModal() {
        this.refs.icingaConfigModal.open();
    },

    _closeModal() {
        this.refs.icingaConfigModal.close();
    },

    _resetConfig() {
        this.setState(this.getInitialState());
    },

    _saveConfig() {
        this.props.updateConfig(this.state.config).then(() => {
            this._closeModal();
        });
    },

    render() {
        return (
            <div>
                <h3>Icinga Plugin Configuration</h3>

                <p>
                    Icinga cluster configuration for all outputs provided by this plugin.
                </p>

                <dl className="deflist">
                    <dt>Icinga Endpoints:</dt>
                    <dd>
                        {this.state.config.icinga_endpoints.length
                            ? this.state.config.icinga_endpoints.join(', ')
                            : '[not set]'}
                    </dd>

                    <dt>Icinga User:</dt>
                    <dd>
                        {this.state.config.icinga_user.length
                            ? this.state.config.icinga_user
                            : '[not set]'}
                    </dd>

                    <dt>Icinga Password:</dt>
                    <dd>
                        {this.state.config.icinga_passwd.length
                            ? '***********'
                            : '[not set]'}
                    </dd>

                    <dt>Verify SSL:</dt>
                    <dd>
                        {this.state.config.verify_ssl
                            ? 'Enabled'
                            : 'Disabled'}
                    </dd>

                    <dt>SSL CA PEM:</dt>
                    <dd>
                        {this.state.config.ssl_ca_pem.length
                            ? this.state.config.ssl_ca_pem
                            : '[not set]'}
                    </dd>
                </dl>

                <IfPermitted permissions="clusterconfigentry:edit">
                    <Button bsStyle="info" bsSize="xs" onClick={this._openModal}>
                        Configure
                    </Button>
                </IfPermitted>

                <BootstrapModalForm
                    ref="icingaConfigModal"
                    title="Update Icinga Plugin Configuration"
                    onSubmitForm={this._saveConfig}
                    onModalClose={this._resetConfig}
                    submitButtonText="Save">
                    <fieldset>
                        <Input
                            id="icinga-endpoints"
                            type="text"
                            label="Icinga Endpoints"
                            help={
                                <span>
                </span>
                            }
                            name="icinga_endpoints"
                            value={this.state.config.icinga_endpoints}
                            onChange={this._onUpdate('icinga_endpoints')}
                        />
                        <Input
                            id="icinga-user"
                            type="text"
                            label="Icinga User"
                            help={
                                <span>
                </span>
                            }
                            name="icinga_user"
                            value={this.state.config.icinga_user}
                            onChange={this._onUpdate('icinga_user')}
                        />
                        <Input
                            id="icinga-passwd"
                            type="text"
                            label="Icinga Password"
                            help={
                                <span>
                </span>
                            }
                            name="icinga_passwd"
                            value={this.state.config.icinga_passwd}
                            onChange={this._onUpdate('icinga_passwd')}
                        />
                        <Input
                            id="icinga-verify-ssl"
                            type="checkbox"
                            label="Verify SSL"
                            help={
                                <span>
                </span>
                            }
                            name="verify_ssl"
                            value={this.state.config.verify_ssl}
                            onChange={this._onUpdate('verify_ssl')}
                        />
                        <Input
                            id="icinga-ssl-ca-pem"
                            type="text"
                            label="SSL CA PEM"
                            help={
                                <span>
                </span>
                            }
                            name="ssl_ca_pem"
                            value={this.state.config.ssl_ca_pem}
                            onChange={this._onUpdate('ssl_ca_pem')}
                        />
                    </fieldset>
                </BootstrapModalForm>
            </div>
        );
    },
});

export default IcingaPluginConfig;
