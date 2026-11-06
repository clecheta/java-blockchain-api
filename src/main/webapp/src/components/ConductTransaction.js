import React, { Component } from 'react';
import { FormGroup, FormControl, Button, ControlLabel, HelpBlock } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import history from '../history';

class ConductTransaction extends Component {
    state = { uid: '', recipient: '', amount: 0, error: '' };

    updateUid = event => {
        this.setState({ uid: event.target.value });
    }

    updateRecipient = event => {
        this.setState({ recipient: event.target.value });
    }

    updateAmount = event => {
        this.setState({ amount: Number(event.target.value) });
    }

    conductTransaction = () => {
        const { uid, recipient, amount } = this.state;

        if (!uid || !recipient || !amount) {
            this.setState({ error: 'Fill uid (sender), recipient (address), and amount.' });
            return;
        }

        fetch(`${document.location.origin}/p00L/tRaN5ACt`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ uid, recipient, amount, type: 'WALLET' })
        }).then(response => {
            if (!response.ok) {
                return response.text().then(t => { throw new Error(t || response.statusText); });
            }
            return response.json();
        }).then(() => {
            alert('Transaction added to pool');
            history.push('/transaction-pool');
        }).catch(err => {
            this.setState({ error: String(err.message || err) });
        });
    }

    render() {
        return (
            <div className='ConductTransaction'>
                <Link to='/'>Home</Link>
                <h3>Conduct a Transaction</h3>
                <p>
                    From <code>./scripts/init-local-wallets.sh</code>:
                    use Alice <strong>uid</strong> as sender, Bob <strong>address</strong> as recipient.
                </p>
                <br />
                <FormGroup>
                    <ControlLabel>uid (sender)</ControlLabel>
                    <FormControl
                        type='text'
                        placeholder='alice uid from init-local-wallets.sh'
                        value={this.state.uid}
                        onChange={this.updateUid}/>
                    <HelpBlock>Private id of the wallet that pays. Never use address here.</HelpBlock>
                </FormGroup>
                <FormGroup>
                    <ControlLabel>recipient (address)</ControlLabel>
                    <FormControl
                        type='text'
                        placeholder='bob address from init-local-wallets.sh'
                        value={this.state.recipient}
                        onChange={this.updateRecipient}/>
                    <HelpBlock>Public key of the wallet that receives. Never use uid here.</HelpBlock>
                </FormGroup>
                <FormGroup>
                    <ControlLabel>amount</ControlLabel>
                    <FormControl
                        type='number'
                        placeholder='amount'
                        value={this.state.amount}
                        onChange={this.updateAmount}/>
                </FormGroup>
                {this.state.error && <p style={{color: 'crimson'}}>{this.state.error}</p>}
                <div>
                    <Button
                        bsStyle="danger"
                        onClick={this.conductTransaction}>
                        Submit
                    </Button>
                </div>
            </div>
        )
    }
};

export default ConductTransaction;
