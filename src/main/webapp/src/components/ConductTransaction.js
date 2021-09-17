import React, { Component } from 'react';
import { FormGroup, FormControl, Button } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import history from '../history';

class ConductTransaction extends Component {
    state = { recipient: '', amount: 0, knownAddresses: [] };

    updateRecipient = event => {
        this.setState({ recipient: event.target.value });
    }

    updateAmount = event => {
        this.setState({ amount: Number(event.target.value) });
    }

    conductTransaction = () => {
        const { recipient, amount } = this.state;

        fetch(`${document.location.origin}/p00L/tRaN5ACt`, {
        // fetch(`http://localhost:8018/p00L/tRaN5ACt`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ recipient, amount })
        }).then(response => response.json())
            .then(json => {
                alert('success');
                history.push('/transaction-pool');
            });
    }

    render() {
        // console.log('this.state', this.state);
        return (
            <div className='ConductTransaction'>
                <Link to='/'>Home</Link>
                <h3>Conduct a Transaction</h3>
                <br />
                <h4>Known Addresses</h4>
                {
                    this.state.knownAddresses.map(knownAddress => {
                        return (
                            <div key={knownAddress}>
                                <div>{knownAddress}</div>
                                <br />
                            </div>
                        );
                    })
                }
                <br />
                <FormGroup>
                    <FormControl
                        input='text'
                        placeholder='recipient'
                        value={this.state.recipient}
                        onChange={this.updateRecipient}/>
                </FormGroup>
                <FormGroup>
                    <FormControl
                        input='number'
                        placeholder='amount'
                        value={this.state.amount}
                        onChange={this.updateAmount}/>
                </FormGroup>
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